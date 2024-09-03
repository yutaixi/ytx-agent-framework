package com.ytx.ai.agent.skill;


import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
import com.plexpt.chatgpt.entity.chat.Message;
import com.ytx.ai.agent.AgentHolder;
import com.ytx.ai.agent.config.AgentProperty;
import com.ytx.ai.agent.dto.ChatDTO;
import com.ytx.ai.agent.execute.AgentExecuteContext;
import com.ytx.ai.agent.vo.AgentTask;
import com.ytx.ai.agent.vo.SubAgentResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class ReplyToUserAgent extends BaseAgent {

    public static final String NAME="reply_to_user_agent";

    public static final String REPLY_AGENT_SYS_PROMPT="reply_agent_sys_prompt";
    public static final String REPLY_AGENT_USER_PROMPT="reply_agent_user_prompt";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return agentProperty.getReplyAgentDesc();
    }

    @Autowired
    AgentProperty agentProperty;
    @Autowired
    AgentHolder agentHolder;

    @Override
    public SubAgentResponse execute(ChatDTO chatDTO, AgentTask task, AgentExecuteContext context) {
        super.execute(chatDTO, task, context);
        List<Message> messages=new ArrayList<>();
        messages.add(Message.ofSystem(getSysPrompt()));
        if(ObjectUtil.isNotEmpty(chatDTO.getHistory()))
        {
            messages.addAll(chatDTO.getHistory());
        }
        String userPrompt=String.format(
                agentHolder.getProperty(REPLY_AGENT_USER_PROMPT),
                task.getObjective(),
                context.getMemoryStr(),
                chatDTO.getQuestion(),
                JSONUtil.toJsonStr(task.getParams())
                );
        messages.add(Message.of(userPrompt));

        ChatCompletion chatCompletion=ChatCompletion.builder()
                .model(ChatCompletion.Model.GPT4o)
                .messages(messages)
                .temperature(CHAT_TEMPERATURE)
                .build();
        ChatCompletionResponse response= llmService.chatCompletion(chatCompletion);
        String reply=response.getChoices().get(0).getMessage().getContent();
        return SubAgentResponse.builder().agentName(getName())
                .result(reply)
                .build();
    }

    private String getSysPrompt()
    {
        return agentHolder.getProperty(REPLY_AGENT_SYS_PROMPT);
    }
}
