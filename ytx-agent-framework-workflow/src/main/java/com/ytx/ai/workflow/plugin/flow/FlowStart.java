package com.ytx.ai.workflow.plugin.flow;


import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.annotation.StartNode;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.plugin.BasicPlugin;
import com.ytx.ai.workflow.plugin.PluginOutput;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

public class FlowStart extends BasicPlugin {

    private Map<String, Value> input;

    @Override
    public void init() {
    }

    @Override
    public String getType() {
        return WorkflowPluginTypeIdEnum.START.getType();
    }

    @Override
    public PluginOutput doBiz(FlowNode flowNode, FlowContext flowContext) {
        return PluginOutput.of();
    }

    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return StartNodeMeta.class;
    }


    @Getter
    @Setter
    @StartNode
    public static class StartNodeMeta implements NodeMeta{

        private List<Value> inputs;

    }
}
