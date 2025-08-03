package com.ytx.ai.workflow.plugin.agent;

import cn.hutool.json.JSONUtil;
import com.ytx.ai.agent.llm.service.LlmService;
import com.ytx.ai.agent.llm.vo.LlmChatCompletion;
import com.ytx.ai.base.agent.ChatDTO;
import com.ytx.ai.base.agent.PlannedTasks;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.annotation.ContractOutputs;
import com.ytx.ai.workflow.annotation.DependsRef;
import com.ytx.ai.workflow.annotation.DependsVariable;
import com.ytx.ai.workflow.annotation.ExpandInputs;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.plugin.BasicPlugin;
import com.ytx.ai.workflow.plugin.PluginOutput;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class AgentReplyPlugin extends BasicPlugin {

    @Autowired
    private LlmService llmService;

    @Override
    public void init() {

    }

    @Override
    public String getType() {
        return WorkflowPluginTypeIdEnum.AGENT_REPLY.getType();
    }


    private static final String USER_PROMPT= """
            Your memory is as follows:
            %s
            The last thing the user said:%s
            The task and results are as follows:
            %s""";

    @Override
    public PluginOutput doBiz(FlowNode flowNode, FlowContext flowContext) {
        System.out.println("AgentReplyPlugin.doBiz");
        AgentReplyNodeMeta meta = (AgentReplyNodeMeta) flowNode.getMeta();
        ChatDTO chat = flowContext.getChat();
        PlannedTasks taskResult=meta.getTaskResult();

        String userPrompt=formatUserPrompt(chat,taskResult);
        LlmChatCompletion chatCompletion=LlmChatCompletion.builder()
                .model(meta.getModelCode())
                .systemPrompt(meta.getSystemPrompt())
                .userPrompt(userPrompt)
                .history(chat.getHistory())
                .build();

        String llmResponse=llmService.chatCompletion(chatCompletion);
        meta.setReply(llmResponse);
        return PluginOutput.of();
    }

    private String formatUserPrompt(ChatDTO chat,PlannedTasks taskResult){
        return  USER_PROMPT.formatted("",chat.getQuestion(), JSONUtil.toJsonStr(taskResult));
    }

    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return AgentReplyNodeMeta.class;
    }

    @Getter
    @Setter
    public static class AgentReplyNodeMeta implements NodeMeta {


        @DependsRef
        private List<Value> inputs;
        private List<Value> outputs;

        @ExpandInputs
        private ChatDTO userInput;
        @ExpandInputs
        private PlannedTasks taskResult;

        @ContractOutputs
        private String reply;

        private String modelCode;
        @DependsVariable
        private String systemPrompt;
    }
}
