package com.ytx.ai.workflow.node.internal;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.agent.service.SkillToolService;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.annotation.DependsRef;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.node.BasicNode;
import com.ytx.ai.workflow.node.NodeOutput;
import com.ytx.ai.workflow.util.ValueUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginNode extends BasicNode {

    @Autowired
    private SkillToolService skillToolService;

    @Override
    public void init() {

    }

    @Override
    public String getType() {
        return WorkflowPluginTypeIdEnum.PLUGIN.getType();
    }

    @Override
    public NodeOutput doBiz(FlowNode flowNode, FlowContext flowContext) {
        System.out.println("PluginNode doBiz");
        PluginNodeMeta nodeMeta= (PluginNodeMeta)flowNode.getMeta();
        Map<String,Object> params=parseParams(nodeMeta.getInputs());
        Object result=null;
        try{
            result=skillToolService.run(nodeMeta.getSkillToolId(),params);
        } catch (Exception e) {
            throw new RuntimeException(e);

        }
        ValueUtils.result2Outputs(result,nodeMeta.getOutputs());
        return NodeOutput.of();
    }

    private Map<String,Object> parseParams(List<Value> inputs){
        Map<String,Object> params=new HashMap<>();
        if(ObjectUtil.isEmpty(inputs)){
            return params;
        }
        inputs.forEach(item->{
            params.put(item.getName(),item.getContent());
        });
        return params;
    }

    @Getter
    @Setter
    public static class PluginNodeMeta implements NodeMeta {
        Integer pluginId;
        Integer skillId;
        Integer skillToolId;
        @DependsRef
        List<Value> inputs;
        List<Value> outputs;
    }

    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return PluginNodeMeta.class;
    }
}
