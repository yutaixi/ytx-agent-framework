package com.ytx.ai.agent;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
import com.plexpt.chatgpt.entity.chat.Message;
import com.plexpt.chatgpt.entity.chat.ResponseFormat;
import com.ytx.ai.agent.Intention.Intention;
import com.ytx.ai.agent.Intention.OtherIntent;
import com.ytx.ai.agent.config.PlanAgentProperty;
import com.ytx.ai.agent.constant.AgentConstants;
import com.ytx.ai.agent.dto.ChatDTO;
import com.ytx.ai.agent.service.IntentionTaskService;
import com.ytx.ai.agent.skill.AiAgent;
import com.ytx.ai.agent.skill.ReplyToUserAgent;
import com.ytx.ai.agent.util.StringUtils;
import com.ytx.ai.agent.vo.AgentTask;
import com.ytx.ai.agent.vo.PlannedTasks;
import com.ytx.ai.agent.vo.UserIntention;
import com.ytx.ai.llm.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import static com.ytx.ai.agent.constant.AgentConstants.JSON_OBJECT;

@Slf4j
public class PlanAgent {

    public static final String SYS_PROMPT="plan_agent_sys_prompt";

    public static final String COMMON_FEW_SHOTS="plan_agent_common_few_shots";

    @Autowired
    LlmService llmService;

    @Autowired
    PlanAgentProperty planAgentProperty;

    @Autowired
    IntentionTaskService intentionTaskService;

    @Autowired
    AgentHolder agentHolder;

    public PlannedTasks run(ChatDTO chatDTO, UserIntention intention){

        PlannedTasks plannedTasks;
        if(ObjectUtil.isNotEmpty(intention)){
            plannedTasks=generateIntentionTasks(chatDTO,intention);
        }else{
            plannedTasks=llmGenerateTasks(chatDTO);
        }
        return plannedTasks;
    }


    private PlannedTasks generateIntentionTasks(ChatDTO chatDTO,UserIntention userIntent){

        PlannedTasks plannedTasks=null;
        Intention intentInstruction=agentHolder.getRefIntention(userIntent.getTag());
        if(!OtherIntent.NAME.equalsIgnoreCase(userIntent.getTag()) && ObjectUtil.isNotEmpty(intentInstruction))
        {
            plannedTasks=intentionTaskService.findPlannedTasks(intentInstruction);
        }else{
            plannedTasks=llmGenerateTasks(chatDTO,userIntent,intentInstruction);
        }
        if(ObjectUtil.isEmpty(plannedTasks) || ObjectUtil.isEmpty(plannedTasks.getTasks())){
            plannedTasks=llmGenerateTasks(chatDTO,userIntent,intentInstruction);
        }
        return plannedTasks;
    }

    private PlannedTasks llmGenerateTasks(ChatDTO chatDTO){

        return llmGenerateTasks(chatDTO,null,null);
    }

    private PlannedTasks llmGenerateTasks(ChatDTO chatDTO, UserIntention userIntent, Intention intentionInstruction){
        List<Message> messages=new ArrayList<>();
        messages.add(Message.ofSystem(getSysPrompt(intentionInstruction)));
        if(ObjectUtil.isNotEmpty(chatDTO.getHistory())){
            messages.addAll(chatDTO.getHistory());
        }
        messages.add(Message.of(getUserPrompt(chatDTO,userIntent)));
        ChatCompletion chatCompletion=ChatCompletion.builder()
                .model(ChatCompletion.Model.GPT4o)
                .messages(messages)
                .temperature(planAgentProperty.getChatTemperature())
                .responseFormat(ResponseFormat.builder().type(JSON_OBJECT).build())
                .build();
        ChatCompletionResponse response=llmService.chatCompletion(chatCompletion);
        if(ObjectUtil.isNull(response))
        {
            log.error(AgentConstants.CHAT_TRACE_LOG_MARKER+":{},{} run chatCompletion failed.",chatDTO.getChatId(),"PlanAgent");
        }
        String jsonData=String.valueOf(response.getChoices().get(0).getMessage().getContent());

        PlannedTasks plannedTasks= JSONUtil.toBean(jsonData,PlannedTasks.class);
        if(ObjectUtil.isEmpty(plannedTasks) || ObjectUtil.isEmpty(plannedTasks.getTasks())){
            plannedTasks=regenerateTask(jsonData);
        }
        return plannedTasks;
    }

    private PlannedTasks regenerateTask(String taskJson){
        if(StringUtils.isBlank(taskJson)){
            return null;
        }
        PlannedTasks plannedTasks=new PlannedTasks();
        AgentTask agentTask=new AgentTask();
        agentTask.setAgent(ReplyToUserAgent.NAME);
        agentTask.setTask_id("1");
        agentTask.setObjective(taskJson);
        agentTask.setExecute_order(1);
        plannedTasks.setTasks(ListUtil.toList(agentTask));
        log.debug("regenerate tasks:{}",plannedTasks);
        return plannedTasks;
    }

    private String getSysPrompt(Intention intent){

        return agentHolder.getProperty(SYS_PROMPT)
                .replace("{{availableSkills}}",getAvailableSkills())
                .replace("{{fewShots}}",getFewShots(intent))
                .replace("{{skillNames}}",getAllAgentNames());

    }

    public String getAvailableSkills(){
        StringBuilder skills=new StringBuilder();
        List<AiAgent> refSkills=agentHolder.getRefSkills();
        if(ObjectUtil.isEmpty(refSkills)){
            return skills.toString();
        }
        refSkills.forEach(agent -> {
            if(!agent.isParticipateLlmPlanning()){
                return;
            }
            skills.append("- ").append(agent.getName()).append(": ").append(agent.getDescription()).append("\n");
        });
        return skills.toString();
    }

    public String getAllAgentNames()
    {
        List<AiAgent> refSkills=agentHolder.getRefSkills();
        if(ObjectUtil.isEmpty(refSkills)){
            return "";
        }
        StringJoiner joiner=new StringJoiner(",");
        refSkills.forEach(aiAgent->{
            joiner.add(aiAgent.getName());
        });
        return joiner.toString();
    }

    private String getFewShots(Intention intent){
        if(ObjectUtil.isNull(intent) || StringUtils.isBlank(intent.getInstruction())){
            return agentHolder.getProperty(COMMON_FEW_SHOTS);
        }
        return intent.getInstruction();
    }

    private String getUserPrompt(ChatDTO chatDTO,UserIntention userIntent){
        StringBuilder prompt=new StringBuilder();
        if(ObjectUtil.isNotEmpty(userIntent) && !StringUtils.isBlank(userIntent.getIntent())){
            prompt.append("user intent:").append(userIntent.getIntent()).append("\n");
        }
        prompt.append("user last input:").append(chatDTO.getQuestion());
        return prompt.toString();
    }
}
