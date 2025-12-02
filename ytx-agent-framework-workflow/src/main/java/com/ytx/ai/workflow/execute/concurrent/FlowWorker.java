package com.ytx.ai.workflow.execute.concurrent;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.jd.platform.async.callback.ICallback;
import com.jd.platform.async.callback.IWorker;
import com.jd.platform.async.worker.WorkResult;
import com.jd.platform.async.wrapper.WorkerWrapper;
import com.ytx.ai.agent.entity.SkillEntity;
import com.ytx.ai.agent.service.SkillService;
import com.ytx.ai.workflow.*;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.execute.FlowExecutor;
import com.ytx.ai.workflow.execute.WorkflowWrapper;
import com.ytx.ai.workflow.plugin.WorkflowPlugin;
import com.ytx.ai.workflow.plugin.PluginOutput;
import com.ytx.ai.workflow.register.WorkflowPluginRegister;
import com.ytx.ai.workflow.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class FlowWorker implements IWorker<FlowWorkerParam, NodeOutput>, ICallback<FlowWorkerParam, NodeOutput> {

    @Override
    public void begin() {
        // Implementation for begin
    }

    @Override
    public NodeOutput action(FlowWorkerParam flowWorkerParam, Map<String, WorkerWrapper> map) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        FlowNode flowNode = flowWorkerParam.getFlowNode();
        if (flowNode == null) {
            return null;
        }

        NodeOutput output = NodeOutput.builder().build();

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
                WorkflowPlugin workflowPlugin = WorkflowPluginRegister.get(flowNode.getComponentId());
                PluginOutput pluginOutput = workflowPlugin.run(flowWorkerParam.getFlowNode(), flowWorkerParam.getFlowContext());
                output.setData(pluginOutput.getData());
                output.setNodeMeta(pluginOutput.getNodeMeta());
                output.setAnswer(pluginOutput.getAnswer());
                break;
            }
        }

        stopWatch.stop();

        // 统计节点耗时
        output.setCost(stopWatch.getLastTaskTimeMillis());
        return output;
    }

    @Override
    public NodeOutput defaultValue() {
        return null;
    }

    @Override
    public void result(boolean success, FlowWorkerParam flowWorkerParam, WorkResult<NodeOutput> workResult) {
        if (!success) {
            log.error("run failed", workResult.getEx());
        }

        WorkflowOutput workFlowOutput = flowWorkerParam.getWorkflowOutput();
        FlowNode node = flowWorkerParam.getFlowNode();
        NodeOutput nodeOutput = workResult.getResult();

        FlowContext context = flowWorkerParam.getFlowContext();
        context.addNodeOutput(node.getId(), nodeOutput);

        if (node.isEndNode()) {
            //end节点的outputs变量输出到工作流输出中
            workFlowOutput.setOutputs(nodeOutput.getData());
            //end节点的answer变量输出到工作流输出中
            if(ObjectUtil.isNotEmpty(nodeOutput.getAnswer())){
                workFlowOutput.setAnswer(nodeOutput.getAnswer());
            }
        }
    }

    private boolean needSkipNode(FlowNode flowNode, FlowContext flowContext) {
        WorkflowWrapper workFlowWrapper = flowContext.getWorkflowWrapper();
        // 开始节点，直接返回不需要跳过
        if (flowNode.isStartNode() || flowNode.isEndNode()) {
            return false;
        }
        List<FlowNode> parentNodes = workFlowWrapper.getParentNodes(flowNode.getId());
        // 所有前置节点执行结果
        Map<String, NodeOutput> nodeOutputMap = flowContext.getNodeOutputMap();
        // 如果所有父节点都跳过，则跳过当前节点
        AtomicBoolean parentNodeAllSkip = new AtomicBoolean(true);
        if (ObjectUtil.isNotEmpty(parentNodes) && ObjectUtil.isNotEmpty(nodeOutputMap)) {
            parentNodes.stream()
                    .map(parentNode -> {
                        return nodeOutputMap.get(parentNode.getId());
                    })
                    .filter(ObjectUtil::isNotNull)
                    .forEach(parentNode -> {
                        parentNodeAllSkip.set(parentNodeAllSkip.get() && parentNode.isSkip());
                    });
        }
        if (parentNodeAllSkip.get()) {
            log.debug("node {} all parent node skipped.", flowNode.getLabel());
            return true;
        }

        List<FlowEdge> incomingEdges = workFlowWrapper.getIncomingEdges(flowNode.getId());
        if (ObjectUtil.isNotEmpty(incomingEdges)) {
            AtomicBoolean anyEdgeConditionsMeet = new AtomicBoolean(false);
            incomingEdges.stream()
                    // .filter(edge -> {
                    //     return ObjectUtil.isNotEmpty(edge.getDepends());
                    // })
                    .filter(edge -> {
                        NodeOutput parentNodeOutput = nodeOutputMap.get(edge.getSource());
                        return parentNodeOutput != null && !parentNodeOutput.isSkip();
                    })
                    .forEach(edge -> {
                        NodeOutput parentNodeOutput = nodeOutputMap.get(edge.getSource());
                        if (ObjectUtil.isNotEmpty(edge.getDepends())) {
                            Value dependsValue = parentNodeOutput.getData().get(edge.getDepends());
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
