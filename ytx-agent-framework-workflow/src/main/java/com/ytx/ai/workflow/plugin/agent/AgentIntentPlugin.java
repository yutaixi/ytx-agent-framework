package com.ytx.ai.workflow.plugin.agent;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.json.JSONUtil;
import com.ytx.ai.agent.llm.service.LlmService;
import com.ytx.ai.agent.llm.vo.LlmChatCompletion;
import com.ytx.ai.base.agent.ChatDTO;
import com.ytx.ai.base.agent.CrisisIdentification;
import com.ytx.ai.base.agent.UserIntention;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.annotation.ContractOutputs;
import com.ytx.ai.workflow.annotation.DependsRef;
import com.ytx.ai.workflow.annotation.DependsVariable;
import com.ytx.ai.workflow.annotation.ExpandInputs;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.enums.ValueTypeEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.plugin.BasicPlugin;
import com.ytx.ai.workflow.plugin.PluginOutput;
import com.ytx.ai.workflow.plugin.agent.vo.IntentDefinition;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class AgentIntentPlugin extends BasicPlugin {

    @Autowired
    private LlmService llmService;

    @Override
    public void init() {

    }

    @Override
    public String getType() {
        return WorkflowPluginTypeIdEnum.AGENT_INTENT.getType();
    }

    @Override
    public PluginOutput doBiz(FlowNode flowNode, FlowContext flowContext) {
        System.out.println("AgentIntentPlugin.doBiz");
        AgentIntentNodeMeta meta = (AgentIntentNodeMeta) flowNode.getMeta();
        ChatDTO chat=meta.getUserInput();
        LlmChatCompletion chatCompletion=LlmChatCompletion.builder()
                .model(meta.getModelCode())
                .systemPrompt(formatSystemPrompt(meta))
                .userPrompt(chat.getQuestion())
                .history(chat.getHistory())
                .jsonFormat()
                .build();

        String llmResponse=llmService.chatCompletion(chatCompletion);
        System.out.println(llmResponse);
        UserIntention userIntention= JSONUtil.toBean(llmResponse,UserIntention.class);
        meta.setUserIntent(userIntention);
        return PluginOutput.of();
    }

    private String formatSystemPrompt(AgentIntentNodeMeta meta){

        String systemPrompt=meta.getSystemPrompt();
        List<IntentDefinition> intents=meta.getIntents();
        if(intents!=null){
            StringBuilder sb=new StringBuilder();
            intents.forEach(intention -> {
                sb.append("- ").append(intention.getTag()).append(":").append(intention.getDesc()).append("\n");
            });
            systemPrompt=systemPrompt.replace("{{intents}}",sb.toString());
        }
        return systemPrompt;
    }

    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return AgentIntentNodeMeta.class;
    }


    @Getter
    @Setter
    public static class AgentIntentNodeMeta implements NodeMeta{

        @DependsRef
        private List<Value> inputs;
        private List<Value> outputs;

        @ExpandInputs
        private ChatDTO userInput;
        @ContractOutputs
        private UserIntention userIntent;


        private String modelCode;
        @DependsVariable
        private String systemPrompt;
        private Double temperature;
        private List<IntentDefinition> intents;

        @DependsVariable
        private String crisisPrompt;
        private CrisisIdentification crisis;

    }
}
