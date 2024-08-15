package com.ytx.ai.agent.skill;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
import com.plexpt.chatgpt.entity.chat.Message;
import com.plexpt.chatgpt.entity.chat.ResponseFormat;
import com.ytx.ai.agent.constant.AgentConstants;
import com.ytx.ai.agent.constant.CommandEnum;
import com.ytx.ai.agent.constant.MemoryConstants;
import com.ytx.ai.agent.dto.ChatDTO;
import com.ytx.ai.agent.enums.MemorySaveOption;
import com.ytx.ai.agent.execute.AgentExecuteContext;
import com.ytx.ai.agent.skill.vo.PreferLanguage;
import com.ytx.ai.agent.util.StringUtils;
import com.ytx.ai.agent.vo.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.ytx.ai.agent.constant.AgentConstants.JSON_OBJECT;

@Slf4j
public class GetUserLanguageAgent extends BaseAgent{

    public static final String NAME="get_user_language_agent";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "获取用户对话沟通想要使用的语言";
    }

    @Override
    public boolean isParticipateLlmPlanning() {
        return false;
    }

    private static final String SYS_PROMPT=
            "You are an agi agent,your task is to get the language that the user wants to use for communication from the input.\n"+
            "Don't assume,don't guess about the Language.\n"+
            "Always return the language in json format,like this:{\"language\",\"\"}.The content of each field in the JSON must be in English.";

    @Override
    public SubAgentResponse execute(ChatDTO chatDTO, AgentTask task, AgentExecuteContext context) {
        super.execute(chatDTO, task, context);
        List<Message> messages=new ArrayList<>();
        messages.add(Message.ofSystem(SYS_PROMPT));
        if(ObjectUtil.isNotEmpty(chatDTO.getHistory())){
            messages.addAll(chatDTO.getHistory());
        }
        messages.add(Message.of(chatDTO.getQuestion()));
        ChatCompletion chatCompletion=ChatCompletion.builder()
                .model(ChatCompletion.Model.GPT4o)
                .messages(messages)
                .temperature(CHAT_TEMPERATURE)
                .responseFormat(ResponseFormat.builder().type(JSON_OBJECT).build())
                .build();
        ChatCompletionResponse response=llmService.chatCompletion(chatCompletion);
        if(ObjectUtil.isNull(response))
        {
            log.error("call llm error");
        }
        String languageJson=String.valueOf(response.getChoices().get(0).getMessage().getContent());
        return afterExecute(context,languageJson);

    }


    private SubAgentResponse afterExecute(AgentExecuteContext context,String preferLanguage)
    {
        SubAgentResponse response=SubAgentResponse.builder()
                .agentName(getName())
                .result(preferLanguage)
                .build();
        PreferLanguage prefer= JSONUtil.toBean(preferLanguage,PreferLanguage.class);
        if(StringUtils.isBlank(prefer.getLanguage()))
        {
            return response;
        }

        Command command=Command.builder()
                .name(CommandEnum.USER_PREFER_LANGUAGE)
                .value(prefer.getLanguage())
                .objective("user prefer language")
                .build();
        MemoryTrace memoryTrace=MemoryTrace.builder()
                .name(MemoryConstants.USER_PREFERRED_LANGUAGE)
                .value(prefer.getLanguage())
                .saveOption(MemorySaveOption.SHORT_TERM)
                .userAssigned(true)
                .build();
        response.setCommands(ListUtil.toList(command));
        response.setMemoryTraces(ListUtil.toList(memoryTrace));
        return response;
    }
}
