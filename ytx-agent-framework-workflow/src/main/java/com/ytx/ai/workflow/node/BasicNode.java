package com.ytx.ai.workflow.node;

import cn.hutool.core.date.StopWatch;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Slf4j
public abstract class BasicNode implements WorkflowNode {

    @PostConstruct
    public void initBean() {
        init();
    }

    abstract public void init();

    private void before(FlowNode flowNode, FlowContext flowContext) {
        checkRequiredParam(flowNode);
        parseParamBeforeRun(flowNode, flowContext);
    }

    /**
     * 检测必要的参数非空
     *
     * @param flowNode
     */
    private void checkRequiredParam(FlowNode flowNode) {
    }

    /**
     * 执行前处理参数
     *
     * @param flowNode
     * @param flowContext
     */
    private void parseParamBeforeRun(FlowNode flowNode, FlowContext flowContext) {
        ValueUtils.resolveRefValues(flowNode.getMeta(), flowContext);
        ValueUtils.resolveVariables(flowNode.getMeta(),flowContext);
        ValueUtils.expandInputs(flowNode.getMeta(),flowContext);
    }

    @Override
    public NodeOutput run(FlowNode flowNode, FlowContext flowContext) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        log.info("node " + flowNode.getLabel() + " start running");

        // 前置处理
        before(flowNode, flowContext);

        // 执行业务逻辑
        NodeOutput response = doBiz(flowNode, flowContext);

        // 后置处理
        after(flowNode, response, flowContext);

        stopWatch.stop();

        log.info("node " + flowNode.getLabel() + " end running.\r\ntime cost: " + stopWatch.getLastTaskTimeMillis() + "ms");

        return response;
    }

    /**
     * 执行后逻辑
     * @param flowNode
     * @param response
     * @param flowContext
     */
    private void after(FlowNode flowNode, NodeOutput response, FlowContext flowContext) {
        response.setNodeMeta(flowNode.getMeta());
        ValueUtils.contractOutputs(flowNode.getMeta(),flowContext);
    }


    protected void setValue(String valueName, Object content, List<Value> values){

    }
}
