package com.ytx.ai.workflow.adaptor.processor;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.Workflow;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.execute.WorkflowWrapper;
import com.ytx.ai.workflow.node.internal.FlowEnd;
import com.ytx.ai.workflow.node.internal.LlmNode;

import java.util.List;
import java.util.stream.Collectors;

public class AlignStreamModePostProcessor implements WorkflowParsePostProcessor{
    @Override
    public void process(Workflow workflow) {
        WorkflowWrapper workflowWrapper=new WorkflowWrapper(workflow);
        FlowNode endNode= workflowWrapper.getEndNode();
        FlowEnd.EndNodeMeta endNodeMeta=(FlowEnd.EndNodeMeta)endNode.getMeta();
        if(!endNodeMeta.isStreamOutput()){
            return;
        }
        List<FlowNode> nodes=workflowWrapper.getParentNodes(endNode.getId());
        List<FlowNode> llmNodes=nodes.stream().filter(node->{
            return WorkflowPluginTypeIdEnum.LLM.getType().equalsIgnoreCase(node.getComponentId());
        }).collect(Collectors.toList());
        if(ObjectUtil.isEmpty(llmNodes) || llmNodes.size()>1){
            return;
        }
        LlmNode.LlmNodeMeta llmNodeMeta=(LlmNode.LlmNodeMeta) llmNodes.getFirst().getMeta();
        llmNodeMeta.setStreaming(true);
    }
}