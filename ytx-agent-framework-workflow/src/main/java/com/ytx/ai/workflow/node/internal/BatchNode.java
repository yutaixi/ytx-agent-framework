package com.ytx.ai.workflow.node.internal;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.base.util.ObjectUtils;
import com.ytx.ai.base.util.StreamUtils;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.NodeResult;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.Workflow;
import com.ytx.ai.workflow.annotation.DependsRef;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.execute.FlowExecutor;
import com.ytx.ai.workflow.node.BasicNode;
import com.ytx.ai.workflow.node.NodeOutput;
import com.ytx.ai.workflow.util.ValueUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Batch node for batch processing
 * 批量处理节点
 */
public class BatchNode extends BasicNode {

    /**
     * 流程执行器，用于执行批量处理中的工作流
     */
    @Autowired
    private FlowExecutor flowExecutor;

    @Override
    public void init() {
        // 初始化逻辑，可根据需要扩展
    }

    @Override
    public String getType() {
        return WorkflowPluginTypeIdEnum.BATCH.getType();
    }

    /**
     * 执行批量处理业务逻辑
     *
     * @param flowNode 流程节点
     * @param flowContext 流程上下文
     * @return 节点输出结果
     */
    @Override
    public NodeOutput doBiz(FlowNode flowNode, FlowContext flowContext) {
        // 获取批量节点的元数据
        BatchNodeMeta meta = (BatchNodeMeta) flowNode.getMeta();

        // 校验元数据和工作流是否为空
        if (ObjectUtil.isEmpty(meta) || ObjectUtil.isEmpty(meta.getWorkflow())) {
            return NodeOutput.of();
        }

        // 校验输入是否为空
        if (ObjectUtil.isEmpty(meta.getInputs())) {
            return NodeOutput.of();
        }

        // 1. Prepare batch tasks (Pivot: Columns -> Rows)
        // 1.准备批量任务(转换:列->行)
        List<BatchTask> tasks = prepareBatchTasks(meta);

        // 计算并行处理数量，如果为空则默认为1
        int parallelCount = meta.getParallelCount() == null ? 1 : meta.getParallelCount();

        // 2. Execute parallel tasks
        // 2.执行并行任务
        StreamUtils.parallel(tasks, (task) -> executeBatchItem(flowNode, flowContext, meta, task), parallelCount);

        // 3. Aggregate Results
        // 3.聚合结果
        if (ObjectUtil.isNotEmpty(meta.getOutputs())) {
            aggregateResults(meta, tasks);
        }

        return NodeOutput.of();
    }

    /**
     * Prepare batch tasks from metadata.
     * 从元数据准备批量任务。
     *
     * @param meta BatchNode metadata
     * @return List of BatchTask
     */
    private List<BatchTask> prepareBatchTasks(BatchNodeMeta meta) {
        // Resolve all input values into lists by name
        Map<String, List<Object>> resolvedInputs = new HashMap<>();
        int maxLen = 0;

        for (Value val : meta.getInputs()) {
            List<Object> currentList = new ArrayList<>();
            if (val.getContent() instanceof Collection<?> col) {
                currentList.addAll(col);
            } else {
                currentList.add(val.getContent());
            }
            resolvedInputs.put(val.getName(), currentList);
            maxLen = Math.max(maxLen, currentList.size());
        }

        // Create tasks by pivoting data
        List<BatchTask> tasks = new ArrayList<>(maxLen);
        for (int i = 0; i < maxLen; i++) {
            Map<String, Object> taskInputs = new HashMap<>();
            for (Value val : meta.getInputs()) {
                List<Object> list = resolvedInputs.get(val.getName());
                // Handle shorter lists by assigning null
                Object value = (i < list.size()) ? list.get(i) : null;
                taskInputs.put(val.getName(), value);
            }
            tasks.add(new BatchTask(taskInputs));
        }

        return tasks;
    }

    /**
     * Execute a single batch task.
     * 执行单个批量任务。
     *
     * @param flowNode The current flow node
     * @param flowContext The parent flow context
     * @param meta BatchNode metadata
     * @param task The batch task to execute
     */
    private void executeBatchItem(FlowNode flowNode, FlowContext flowContext, BatchNodeMeta meta, BatchTask task) {
        // 创建批量处理的上下文,基于当前上下文创建新实例
        FlowContext batchContext = FlowContext.of(flowContext);
        // 设置非严格模式,允许批量处理中的节点不包含开始结束节点。
        batchContext.setStrictMode(false);

        Map<String, NodeResult> batchNodeOutputMap = batchContext.getNodeOutputMap();
        BatchNodeMeta batchMeta = new BatchNodeMeta();

        // Construct inputs for this specific task
        // 构建本任务的输入参数
        List<Value> iterationInputs = new ArrayList<>();
        for (Value def : meta.getInputs()) {
            Object content = task.getInputs().get(def.getName());
            iterationInputs.add(Value.builder()
                    .id(def.getId())
                    .name(def.getName())
                    .type(def.getType())
                    .content(content)
                    .build());
        }

        batchMeta.setInputs(iterationInputs);
        batchNodeOutputMap.put(flowNode.getId(), NodeResult.builder().nodeMeta(batchMeta).build());

        // 执行批量处理工作流
        Workflow batchBodyWorkflow = ObjectUtils.deepClone(meta.getWorkflow());
        flowExecutor.execute(batchBodyWorkflow, batchContext);

        // Capture outputs
        // 获取输出结果
        if (ObjectUtil.isNotEmpty(meta.getOutputs())) {
            List<Value> outputs = ObjectUtils.deepClone(meta.getOutputs());
            outputs.forEach(output -> ValueUtils.resolveRefValue(output, batchContext));
            task.setOutputs(outputs);
        }
    }

    /**
     * Aggregate parallel execution results into the outputs of the metadata.
     * 将并行执行结果聚合到元数据的输出中。
     *
     * @param meta BatchNode metadata
     * @param tasks List of executed tasks
     */
    private void aggregateResults(BatchNodeMeta meta, List<BatchTask> tasks) {
        // Map to store aggregated data lists by output variable name
        // 使用变量名作为key来存储聚合后的数据列表
        Map<String, List<Object>> aggregatedDataMap = new HashMap<>();

        // Initialize lists for all defined outputs
        // 初始化所有输出变量的结果列表
        for (Value output : meta.getOutputs()) {
            aggregatedDataMap.put(output.getName(), new ArrayList<>());
        }

        // Iterate through all tasks to ensure order matches the input sequence
        // 遍历所有任务以确保输出值的顺序与输入值的顺序(inputs的变量顺序)一致
        // Since 'tasks' list preserves the original input order (row 0, row 1...), iterating it guarantees alignment.
        for (BatchTask task : tasks) {
            List<Value> taskOutputs = task.getOutputs();
            if (taskOutputs != null) {
                // Create a temporary map for the current task's results
                Map<String, Object> currentResults = new HashMap<>();
                for (Value v : taskOutputs) {
                    currentResults.put(v.getName(), v.getContent());
                }

                // Add result to corresponding aggregated list
                for (String name : aggregatedDataMap.keySet()) {
                    Object val = currentResults.get(name);
                    aggregatedDataMap.get(name).add(val);
                }
            } else {
                // If task failed or no result, add null to all lists to maintain index alignment
                // 如果任务失败或无结果,所有列表添加 null 以保持索引对齐
                for (List<Object> list : aggregatedDataMap.values()) {
                    list.add(null);
                }
            }
        }

        // Set aggregated content back to meta outputs matching by name
        // 根据变量名将聚合后的内容回填到 meta outputs
        for (Value output : meta.getOutputs()) {
            List<Object> data = aggregatedDataMap.get(output.getName());
            if (data != null) {
                output.setContent(data);
            }
        }
    }

    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return BatchNodeMeta.class;
    }

    /**
     * Batch task holder
     * 批量任务持有对象
     */
    @Getter
    @Setter
    public static class BatchTask {
        /**
         * Input values for this task (Variable Name -> Value)
         * 本任务的输入值(变量名 -> 值)
         */
        private Map<String, Object> inputs;

        /**
         * Output values from this task
         * 本任务的输出值
         */
        private List<Value> outputs;

        public BatchTask(Map<String, Object> inputs) {
            this.inputs = inputs;
        }
    }

    /**
     * Batch node metadata
     * 批量节点元数据
     */
    @Getter
    @Setter
    public static class BatchNodeMeta implements NodeMeta {
        /**
         * 并行处理数量
         * 控制批量处理时的并发度
         */
        private Integer parallelCount;

        /**
         * 最大批量处理数量
         * 限制单次批量处理的最大数据量
         */
        private Integer maxBatchCount;

        /**
         * 批量处理的输入变量列表
         * 使用 @DependsRef 注解标记，表示这些变量需要解析引用关系
         */
        @DependsRef
        private List<Value> inputs;

        /**
         * 批量处理的输出变量列表
         * 使用 @DependsRef 注解标记，表示这些变量需要解析引用关系
         */
        @DependsRef
        private List<Value> outputs;

        /**
         * Inner workflow for batch processing
         * 批量处理的内嵌工作流
         */
        private Workflow workflow;
    }
}

