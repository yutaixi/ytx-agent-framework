package com.ytx.ai.workflow.plugin.flow;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.ytx.ai.agent.entity.SkillEntity;
import com.ytx.ai.agent.service.SkillService;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.Workflow;
import com.ytx.ai.workflow.annotation.DependsRef;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.execute.FlowExecutor;
import com.ytx.ai.workflow.execute.WorkflowWrapper;
import com.ytx.ai.workflow.plugin.BasicPlugin;
import com.ytx.ai.workflow.plugin.PluginOutput;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class SubProcessPlugin extends BasicPlugin {


    @Autowired
    private SkillService skillService;

    @Override
    public void init() {

    }

    @Override
    public String getType() {
        return WorkflowPluginTypeIdEnum.SUBPROCESS.getType();
    }

    @Override
    public PluginOutput doBiz(FlowNode flowNode, FlowContext flowContext) {
        SubProcessNodeMeta subProcessNodeMeta = (SubProcessNodeMeta)flowNode.getMeta();
        String flowIdStr=subProcessNodeMeta.getProcessId();
        WorkflowWrapper workflowWrapper=null;
        Workflow workflow=null;
        if(ObjectUtil.isNotEmpty(flowIdStr)){
            SkillEntity skill=skillService.findSkill(Integer.valueOf(flowIdStr));
            workflow=Workflow.of(skill);
            workflowWrapper=new WorkflowWrapper(workflow);
        }

        FlowNode startNode= workflowWrapper.getStartNode();
        FlowStart.StartNodeMeta startNodeMeta=(FlowStart.StartNodeMeta) startNode.getMeta();
        startNodeMeta.setInputs(subProcessNodeMeta.getInputs());

        FlowContext subFlowContext = FlowContext.of()
                .chat(flowContext.getChat())
                .workflowWrapper(workflowWrapper);
        FlowExecutor flowExecutor = SpringUtil.getBean(FlowExecutor.class);

        flowExecutor.execute(workflow,subFlowContext);

        FlowNode endNode= workflowWrapper.getEndNode();
        FlowEnd.EndNodeMeta endNodeMeta=(FlowEnd.EndNodeMeta) endNode.getMeta();
        subProcessNodeMeta.setOutputs(endNodeMeta.getOutputs());
        subProcessNodeMeta.setOutputText(endNodeMeta.getOutputText());
        PluginOutput subProcessPluginOutput = PluginOutput.of();
        subProcessPluginOutput.setAnswer(endNodeMeta.getOutputText());
        return subProcessPluginOutput;
    }

    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return SubProcessNodeMeta.class;
    }

    @Getter
    @Setter
    public static class SubProcessNodeMeta implements NodeMeta{
        private String processId;
        private String version;
        @DependsRef
        private List<Value> inputs;
        private List<Value> outputs;
        private String outputText;
    }
}
