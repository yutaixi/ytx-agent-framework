package com.ytx.ai.workflow.plugin;

import cn.hutool.core.date.StopWatch;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;

@Slf4j
public abstract class BasicPlugin implements Plugin {

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

    }

    @Override
    public PluginOutput run(FlowNode flowNode, FlowContext flowContext) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        log.info("node " + flowNode.getLabel() + " start running");

        // 前置处理
        before(flowNode, flowContext);

        // 执行业务逻辑
        PluginOutput response = doBiz(flowNode, flowContext);

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
    private void after(FlowNode flowNode, PluginOutput response, FlowContext flowContext) {
        response.setNodeMeta(flowNode.getMeta());

    }
}
