package com.ytx.ai.workflow.plugin.agent;

import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.annotation.EndNode;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.plugin.BasicPlugin;
import com.ytx.ai.workflow.plugin.PluginOutput;
import lombok.Getter;
import lombok.Setter;

public class AgentEndPlugin extends BasicPlugin {
    @Override
    public void init() {

    }

    @Override
    public String getType() {
        return WorkflowPluginTypeIdEnum.AGENT_END.getType();
    }

    @Override
    public PluginOutput doBiz(FlowNode flowNode, FlowContext flowContext) {
        System.out.println("AgentEndPlugin.doBiz");
        return PluginOutput.of();
    }

    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return AgentEndNodeMeta.class;
    }

    @Getter
    @Setter
    @EndNode
    public static class AgentEndNodeMeta  implements NodeMeta{

        private String content;

    }
}
