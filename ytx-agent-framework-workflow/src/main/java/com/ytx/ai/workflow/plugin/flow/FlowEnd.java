package com.ytx.ai.workflow.plugin.flow;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.annotation.DependsRef;
import com.ytx.ai.workflow.annotation.DependsVariable;
import com.ytx.ai.workflow.annotation.EndNode;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.plugin.BasicPlugin;
import com.ytx.ai.workflow.plugin.PluginOutput;
import com.ytx.ai.workflow.util.ValueUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class FlowEnd extends BasicPlugin {


    @Override
    public void init() {
    }

    @Override
    public String getType() {
        return WorkflowPluginTypeIdEnum.END.getType();
    }

    @Override
    public PluginOutput doBiz(FlowNode flowNode, FlowContext flowContext) {

        EndNodeMeta nodeMeta= (EndNodeMeta) flowNode.getMeta();

        PluginOutput output = PluginOutput.of();
        output.setData(ValueUtils.toMap(nodeMeta.getOutputs()));
        String outputText=nodeMeta.getOutputText();
        if(ObjectUtil.isNotEmpty(outputText)){
            output.setAnswer(outputText);
        }
        return output;
    }

    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return EndNodeMeta.class;
    }


    @Getter
    @Setter
    @EndNode
    public static class EndNodeMeta implements NodeMeta {

        private String returnType;
        @DependsRef
        private List<Value> outputs;
        @DependsVariable
        private String outputText;
    }
}
