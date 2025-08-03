package com.ytx.ai.workflow.plugin.agent;

import cn.hutool.core.util.ObjectUtil;
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

public class AgentStartPlugin extends BasicPlugin {
    @Override
    public void init() {

    }

    @Override
    public String getType() {
        return WorkflowPluginTypeIdEnum.AGENT_START.getType();
    }

    @Override
    public PluginOutput doBiz(FlowNode flowNode, FlowContext flowContext) {
        System.out.println("AgentStartPlugin.doBiz");
        AgentStartNodeMeta meta = (AgentStartNodeMeta) flowNode.getMeta();
        if(ObjectUtil.isNotEmpty(meta.getInputs())){
            meta.getInputs().stream().filter(input->"userInput".equalsIgnoreCase(input.getName())).findFirst().ifPresent(input->{
                input.setContent(flowContext.getChat());
            });
        }
        return PluginOutput.of();
    }

    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return AgentStartNodeMeta.class;
    }

    @Getter
    @Setter
    @StartNode
    public static class AgentStartNodeMeta  implements NodeMeta{
        private List<Value> inputs;
    }
}
