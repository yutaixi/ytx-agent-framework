package com.ytx.ai.workflow.execute.concurrent;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jd.platform.async.callback.ICallback;
import com.jd.platform.async.callback.IWorker;
import com.jd.platform.async.worker.WorkResult;
import com.jd.platform.async.wrapper.WorkerWrapper;
import com.ytx.ai.agent.entity.SkillEntity;
import com.ytx.ai.agent.service.SkillService;
import com.ytx.ai.workflow.*;
import com.ytx.ai.workflow.enums.ComponentTypeEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.execute.FlowExecutor;
import com.ytx.ai.workflow.execute.WorkflowWrapper;
import com.ytx.ai.workflow.node.WorkflowNode;
import com.ytx.ai.workflow.node.NodeOutput;
import com.ytx.ai.workflow.register.WorkflowPluginRegister;
import com.ytx.ai.workflow.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class FlowWorker implements IWorker<FlowWorkerParam, NodeResult>, ICallback<FlowWorkerParam, NodeResult> {

    /** 节点跟踪状态常量 */
    private static final String TRACE_STATUS_SUCCESS = "success";
    private static final String TRACE_STATUS_SKIPPED = "skipped";
    private static final String TRACE_STATUS_ERROR   = "error";

    @Override
    public void begin() {
        // Implementation for begin
    }

    @Override
    public NodeResult action(FlowWorkerParam flowWorkerParam, Map<String, WorkerWrapper> map) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        FlowNode flowNode = flowWorkerParam.getFlowNode();
        if (flowNode == null) {
            return null;
        }

        NodeResult output = NodeResult.builder().build();

        // 如果该条件不满足，或者父节点全部跳过，则跳过当前节点
        if (needSkipNode(flowNode, flowWorkerParam.getFlowContext())) {
            output.setSkip(true);

            // 统计节点耗时
            stopWatch.stop();
            output.setCost(stopWatch.getLastTaskTimeMillis());
            log.debug("node {} skipped.", flowNode.getLabel());
            return output;
        }

        // 判断节点类型
        switch (flowNode.getComponentType()) {
            // workflow类型
            case workflow: {
                SkillService skillService = SpringUtil.getBean(SkillService.class);
                Integer skillId = Integer.valueOf(flowNode.getComponentId());

                // 获取子流程对应的流程信息
                SkillEntity skill = skillService.findSkill(skillId);

                // 转成workflow对象
                Workflow workflow = Workflow.of(skill);

                // 处理开始节点调用参数，替换为实际值
                WorkflowWrapper workFlowWrapper = new WorkflowWrapper(workflow);
                FlowContext parentFlowContext = flowWorkerParam.getFlowContext();
                FlowNode startNode = workFlowWrapper.getStartNode();
                ValueUtils.resolveRefValues(startNode.getMeta(), parentFlowContext);

                // 新建子流程context，集成父流程的对话上下文
                FlowContext subFlowContext = FlowContext.of()
                        .chat(parentFlowContext.getChat())
                        .workflowWrapper(workFlowWrapper);

                // 获取流程执行器，并执行子流程
                FlowExecutor flowExecutor = SpringUtil.getBean(FlowExecutor.class);
                WorkflowOutput subWorkFlowOutput = flowExecutor.execute(workflow, subFlowContext);
                output.setData(subWorkFlowOutput.getOutputs());
                output.setAnswer(subWorkFlowOutput.getAnswer());
                break;
            }

            // 插件类型，默认认为插件
            case plugin:
            default: {
                WorkflowNode workflowNode = WorkflowPluginRegister.get(flowNode.getComponentId());
                NodeOutput nodeOutput = workflowNode.run(flowWorkerParam.getFlowNode(), flowWorkerParam.getFlowContext());
                output.setData(nodeOutput.getData());
                output.setNodeMeta(nodeOutput.getNodeMeta());
                output.setAnswer(nodeOutput.getAnswer());
                break;
            }
        }

        stopWatch.stop();

        // 统计节点耗时
        output.setCost(stopWatch.getLastTaskTimeMillis());
        return output;
    }

    @Override
    public NodeResult defaultValue() {
        return null;
    }

    /**
     * 节点执行完成回调（无论成功或失败都会触发）。
     * <p>
     * 主要职责：
     * <ol>
     *   <li>将节点结果写入 FlowContext，供下游节点引用。</li>
     *   <li>若为 End 节点，将输出同步到 WorkflowOutput。</li>
     *   <li>构建节点执行跟踪信息（{@link NodeTraceInfo}），写入 WorkflowOutput，供调试查看。</li>
     * </ol>
     *
     * @param success         节点是否执行成功
     * @param flowWorkerParam 节点执行参数（含节点定义、上下文、输出容器）
     * @param workResult      节点执行结果（含 NodeResult 或异常信息）
     */
    @Override
    public void result(boolean success, FlowWorkerParam flowWorkerParam, WorkResult<NodeResult> workResult) {
        WorkflowOutput workFlowOutput = flowWorkerParam.getWorkflowOutput();
        FlowNode node = flowWorkerParam.getFlowNode();
        NodeResult nodeResult = workResult.getResult();
        Throwable executionError = null;

        if (!success) {
            executionError = workResult.getEx();
            log.error("node {} run failed", node.getLabel(), executionError);
            // 确保 nodeResult 不为 null，防止后续 NPE
            if (nodeResult == null) {
                nodeResult = NodeResult.builder()
                        .errorMessage(executionError != null ? executionError.getMessage() : "未知错误")
                        .build();
            } else {
                nodeResult.setErrorMessage(executionError != null ? executionError.getMessage() : "未知错误");
            }
        }

        FlowContext context = flowWorkerParam.getFlowContext();
        context.addNodeOutput(node.getId(), nodeResult);

        if (node.isEndNode() && nodeResult != null) {
            // end节点的outputs变量输出到工作流输出中
            workFlowOutput.setOutputs(nodeResult.getData());
            // end节点的answer变量输出到工作流输出中
            if (ObjectUtil.isNotEmpty(nodeResult.getAnswer())) {
                workFlowOutput.setAnswer(nodeResult.getAnswer());
            }
        }

        // 构建并存储节点执行跟踪信息，用于前端调试展示
        NodeTraceInfo traceInfo = buildNodeTraceInfo(node, nodeResult, executionError);
        workFlowOutput.addNodeTrace(node.getId(), traceInfo);
    }

    /**
     * 构建节点执行跟踪信息。
     * <p>
     * 参数说明：
     * <ul>
     *   <li>{@code node}：节点定义，提供 id、label、componentType/componentId。</li>
     *   <li>{@code nodeResult}：节点执行结果，提供 cost、skip、data（outputs）、nodeMeta（inputs）。</li>
     *   <li>{@code error}：执行异常（非 null 表示执行失败）。</li>
     * </ul>
     *
     * @param node       当前节点定义
     * @param nodeResult 节点执行结果（可为 null，执行异常时可能未返回）
     * @param error      执行异常（成功时为 null）
     * @return 节点执行跟踪信息
     */
    private NodeTraceInfo buildNodeTraceInfo(FlowNode node, NodeResult nodeResult, Throwable error) {
        // 确定节点类型字符串
        String nodeType = ComponentTypeEnum.plugin.equals(node.getComponentType())
                ? node.getComponentId()
                : "workflow";

        // 确定执行状态
        String status;
        if (error != null) {
            status = TRACE_STATUS_ERROR;
        } else if (nodeResult != null && nodeResult.isSkip()) {
            status = TRACE_STATUS_SKIPPED;
        } else {
            status = TRACE_STATUS_SUCCESS;
        }

        long cost = nodeResult != null ? nodeResult.getCost() : 0;
        String errorMessage = error != null ? error.getMessage() : null;

        Map<String, Object> inputs = extractInputs(nodeResult);
        Map<String, Object> outputs = extractOutputs(nodeResult);

        return NodeTraceInfo.builder()
                .nodeId(node.getId())
                .nodeLabel(node.getLabel())
                .nodeType(nodeType)
                .status(status)
                .cost(cost)
                .errorMessage(errorMessage)
                .inputs(inputs)
                .outputs(outputs)
                .build();
    }

    /**
     * 从节点结果中提取输入信息（变量解析后的实际值）。
     * <p>
     * 将 nodeMeta 序列化为 Map，排除 "outputs" 字段（避免与输出区域重复展示）。
     * 对于 LLM 节点，inputs Map 包含：userPrompt、systemPrompt、modelCode、inputs（List<Value>）等字段。
     *
     * @param nodeResult 节点执行结果
     * @return 输入 Map，key 为字段名，value 为解析后的实际值；无法提取时返回空 Map
     */
    private Map<String, Object> extractInputs(NodeResult nodeResult) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        if (nodeResult == null || nodeResult.getNodeMeta() == null) {
            return inputs;
        }
        try {
            JSONObject metaJson = JSONUtil.parseObj(JSONUtil.toJsonStr(nodeResult.getNodeMeta()));
            metaJson.forEach((k, v) -> {
                // 排除 outputs 字段，避免与输出区域内容重复
                if (!"outputs".equals(k)) {
                    inputs.put(k, v);
                }
            });
        } catch (Exception e) {
            log.warn("节点 inputs 提取失败: {}", e.getMessage());
        }
        return inputs;
    }

    /**
     * 从节点结果中提取输出信息（节点计算后的实际值）。
     * <p>
     * 提取策略：
     * <ol>
     *   <li>优先从 {@code NodeResult.data}（Map&lt;String,Value&gt;）中提取，取变量名 → 变量内容。
     *       适用于 EndNode（data 由 ValueUtils.toMap 填充）。</li>
     *   <li>若 data 为空，则从 nodeMeta 的 "outputs" 字段（List&lt;Value&gt;）中提取。
     *       适用于 LlmNode 等（outputs 内容在 doBiz 中写入 meta，但 data 仍为空 Map）。</li>
     * </ol>
     *
     * @param nodeResult 节点执行结果
     * @return 输出 Map，key 为变量名，value 为变量内容；无法提取时返回空 Map
     */
    private Map<String, Object> extractOutputs(NodeResult nodeResult) {
        Map<String, Object> outputs = new LinkedHashMap<>();
        if (nodeResult == null) {
            return outputs;
        }

        // 策略一：从 NodeResult.data 提取（EndNode 走这里）
        if (ObjectUtil.isNotEmpty(nodeResult.getData())) {
            nodeResult.getData().forEach((k, v) -> outputs.put(k, v != null ? v.getContent() : null));
            return outputs;
        }

        // 策略二：从 nodeMeta.outputs 字段提取（LlmNode 等走这里）
        if (nodeResult.getNodeMeta() == null) {
            return outputs;
        }
        try {
            JSONObject metaJson = JSONUtil.parseObj(JSONUtil.toJsonStr(nodeResult.getNodeMeta()));
            Object metaOutputs = metaJson.get("outputs");
            if (metaOutputs instanceof JSONArray) {
                ((JSONArray) metaOutputs).forEach(item -> {
                    if (item instanceof JSONObject) {
                        JSONObject valueObj = (JSONObject) item;
                        String name = valueObj.getStr("name");
                        Object content = valueObj.get("content");
                        if (name != null) {
                            outputs.put(name, content);
                        }
                    }
                });
            }
        } catch (Exception e) {
            log.warn("节点 outputs 提取失败: {}", e.getMessage());
        }
        return outputs;
    }

    private boolean needSkipNode(FlowNode flowNode, FlowContext flowContext) {
        WorkflowWrapper workFlowWrapper = flowContext.getWorkflowWrapper();
        // 开始节点，直接返回不需要跳过
        if (flowNode.isStartNode() || flowNode.isEndNode()) {
            return false;
        }
        List<FlowNode> parentNodes = workFlowWrapper.getParentNodes(flowNode.getId());
        // 所有前置节点执行结果
        Map<String, NodeResult> nodeOutputMap = flowContext.getNodeOutputMap();
        // 如果所有父节点都跳过，则跳过当前节点
        AtomicBoolean parentNodeAllSkip = new AtomicBoolean(true);
        if (ObjectUtil.isNotEmpty(parentNodes) && ObjectUtil.isNotEmpty(nodeOutputMap)) {
            parentNodes.stream()
                    .map(parentNode -> nodeOutputMap.get(parentNode.getId()))
                    .filter(ObjectUtil::isNotNull)
                    .forEach(parentNode -> parentNodeAllSkip.set(parentNodeAllSkip.get() && parentNode.isSkip()));
        } else {
            parentNodeAllSkip.set(false);
        }
        if (parentNodeAllSkip.get()) {
            log.debug("node {} all parent node skipped.", flowNode.getLabel());
            return true;
        }

        List<FlowEdge> incomingEdges = workFlowWrapper.getIncomingEdges(flowNode.getId());
        if (ObjectUtil.isNotEmpty(incomingEdges)) {
            AtomicBoolean anyEdgeConditionsMeet = new AtomicBoolean(false);
            List<FlowEdge> edgesToProcess = incomingEdges.stream()
                    .filter(edge -> {
                        NodeResult parentNodeResult = nodeOutputMap.get(edge.getSource());
                        return parentNodeResult != null && !parentNodeResult.isSkip();
                    }).collect(Collectors.toList());
            if (ObjectUtil.isEmpty(edgesToProcess)) {
                return false;
            }

            edgesToProcess.forEach(edge -> {
                NodeResult parentNodeResult = nodeOutputMap.get(edge.getSource());
                if (ObjectUtil.isNotEmpty(edge.getDepends())) {
                    Value dependsValue = parentNodeResult.getData().get(edge.getDepends());
                    if (dependsValue == null || ObjectUtil.isEmpty(dependsValue.getContent())) {
                        return;
                    }
                    anyEdgeConditionsMeet.set(anyEdgeConditionsMeet.get() ||
                            Boolean.parseBoolean(dependsValue.getContent().toString()));
                } else {
                    anyEdgeConditionsMeet.set(true);
                }
            });

            if (!anyEdgeConditionsMeet.get()) {
                log.debug("node {} skipped not meet all conditions.", flowNode.getLabel());
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}