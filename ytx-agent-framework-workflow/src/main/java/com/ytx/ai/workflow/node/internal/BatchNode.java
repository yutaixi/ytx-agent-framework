package com.ytx.ai.workflow.node.internal;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.Workflow;
import com.ytx.ai.workflow.annotation.DependsRef;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.execute.FlowExecutor;
import com.ytx.ai.workflow.node.BasicNode;
import com.ytx.ai.workflow.node.NodeOutput;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

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

        // 创建批量处理的上下文，基于当前上下文创建新实例
        FlowContext batchContext = FlowContext.of(flowContext);
        // 设置非严格模式，允许批量处理中的节点执行失败不影响整体流程
        batchContext.setStrictMode(false);

        // 获取批量处理的内嵌工作流
        Workflow workflow = meta.getWorkflow();

        // 执行批量处理工作流
        flowExecutor.execute(workflow, batchContext);

        return NodeOutput.of();
    }

    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return BatchNodeMeta.class;
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

