package com.ytx.ai.workflow.plugin;


import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.enums.PluginTypeIdEnum;
import com.ytx.ai.workflow.execute.FlowContext;
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
        return PluginTypeIdEnum.START.getType();
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
    public static class StartNodeMeta implements NodeMeta{

        private List<Value> inputs;

    }
}
