package com.ytx.ai.workflow.plugin.agent;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.ytx.ai.agent.entity.SkillEntity;
import com.ytx.ai.agent.llm.service.LlmService;
import com.ytx.ai.agent.llm.vo.LlmChatCompletion;
import com.ytx.ai.agent.service.SkillService;
import com.ytx.ai.base.agent.*;
import com.ytx.ai.workflow.*;
import com.ytx.ai.workflow.adaptor.WorkflowAdaptor;
import com.ytx.ai.workflow.annotation.ContractOutputs;
import com.ytx.ai.workflow.annotation.DependsRef;
import com.ytx.ai.workflow.annotation.DependsVariable;
import com.ytx.ai.workflow.annotation.ExpandInputs;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.execute.WorkflowWrapper;
import com.ytx.ai.workflow.plugin.BasicPlugin;
import com.ytx.ai.workflow.plugin.PluginOutput;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

public class AgentPlanPlugin extends BasicPlugin {

    @Autowired
    private LlmService llmService;

    @Autowired
    private SkillService skillService;

    @Override
    public void init() {

    }

    @Override
    public String getType() {
        return WorkflowPluginTypeIdEnum.AGENT_PLAN.getType();
    }

    @Override
    public PluginOutput doBiz(FlowNode flowNode, FlowContext flowContext) {
        System.out.println("AgentPlanPlugin.doBiz");
        AgentPlanNodeMeta meta = (AgentPlanNodeMeta) flowNode.getMeta();

        UserIntention userIntention=meta.getUserIntent();
        ChatDTO userInput=meta.getUserInput();
        Map<String, Skill> skillMap= processSkillMap(meta,flowContext);

        if(ObjectUtil.isNotEmpty(userIntention.getTag()) && ObjectUtil.isNotEmpty(meta.getIntentMapping().get(userIntention.getTag()))){
            Workflow workflow=meta.getIntentMapping().get(userIntention.getTag());
            PlannedTasks tasks=new PlannedTasks();
            AgentTask task=new AgentTask();
            task.setTask_id("1");
            task.setAgent_id(String.valueOf(workflow.getId()));
            task.setFunction(workflow.getName());
            task.setExecute_order(1);
            tasks.setTasks(ListUtil.toList(task));
            meta.setPlannedTasks(tasks);
            return PluginOutput.of();
        }else{

        }
        LlmChatCompletion chatCompletion=LlmChatCompletion.builder()
                .model(meta.getModelCode())
                .systemPrompt(formatSystemPrompt(meta.getSystemPrompt(),skillMap))
                .userPrompt(userInput.getQuestion())
                .history(userInput.getHistory())
                .jsonFormat()
                .build();
        String llmResponse=llmService.chatCompletion(chatCompletion);
        System.out.println("llmResponse:"+llmResponse);
        PlannedTasks tasks= JSONUtil.toBean(llmResponse, PlannedTasks.class);
        meta.setPlannedTasks(tasks);
        return PluginOutput.of();
    }

    private String formatSystemPrompt(String systemPrompt,Map<String, Skill> skillMap){

        String skillStr=skillMapToString(skillMap);
        return systemPrompt.replace("{{agents}}",skillStr);
    }

    private String skillMapToString(Map<String, Skill> skillMap){
        if(ObjectUtil.isEmpty(skillMap)){
            return "[]";
        }
        StringBuilder skillBuilder=new StringBuilder("[");
        skillMap.values().forEach(item->{
            skillBuilder.append("{ id:").append(item.getId())
                    .append(",name:").append(item.getName())
                    .append(",description:").append(ObjectUtil.isEmpty(item.getDescription())?"\"\"":item.getDescription())
                    .append(",arguments:").append(ObjectUtil.isEmpty(item.getArguments())?"\"\"":item.getArguments())
                    .append("}");
        });
        skillBuilder.append("]");
        return skillBuilder.toString();
    }

    private Map<String, Skill> processSkillMap(AgentPlanNodeMeta meta, FlowContext flowContext){
        Map<String, Skill> skillMap=flowContext.getSkillMap();

        Map<String, Workflow> intentMapping=meta.getIntentMapping();
        if(ObjectUtil.isNotEmpty(intentMapping)){
            intentMapping.entrySet().stream()
                    .filter(item->{
                        return ObjectUtil.isNotEmpty(item.getValue());
                    })
                    .forEach(entrySet->{
                        Workflow workflow=entrySet.getValue();
                        Skill skill =skillMap.computeIfAbsent(String.valueOf(workflow.getId()), key->new Skill());
                        SkillEntity skillEntity=skillService.findSkill(workflow.getId());
                        Workflow wf=Workflow.of(skillEntity);
                        skill.setId(String.valueOf(wf.getId()));
                        skill.setType(AgentSkillTypeEnum.WORKFLOW);
                        skill.setName(wf.getName());
                        skill.setDescription(wf.getDescription());

                        WorkflowWrapper workflowWrapper=new WorkflowWrapper(wf);
                        FlowNode startNode= workflowWrapper.getStartNode();
                        NodeMeta nodeMeta= startNode.getMeta();
                        if(ObjectUtil.isNotEmpty(nodeMeta) && ObjectUtil.isNotEmpty(nodeMeta.getInputs())){
                            StringBuilder argumentsBuilder=new StringBuilder("[");
                            nodeMeta.getInputs().forEach(item->{
                                argumentsBuilder.append("{ name:").append(item.getName())
                                        .append(";description:").append(ObjectUtil.isEmpty(item.getDescription())?"\"\"":item.getDescription())
                                        .append(";required:").append(item.getRequired() != null && item.getRequired())
                                        .append(";}");
                            });
                            argumentsBuilder.append("]");
                            skill.setArguments(argumentsBuilder.toString());
                        }
                    });
        }
        return skillMap;
    }

    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return AgentPlanNodeMeta.class;
    }


    @Getter
    @Setter
    public static class AgentPlanNodeMeta implements NodeMeta {
        @DependsRef
        private List<Value> inputs;
        private List<Value> outputs;

        @ExpandInputs
        private UserIntention userIntent;
        @ExpandInputs
        private ChatDTO userInput;
        @ContractOutputs
        private PlannedTasks plannedTasks;

        private String modelCode;
        @DependsVariable
        private String systemPrompt;
        private Double temperature;
        private Map<String, Workflow> intentMapping;

    }
}
