package com.ytx.ai.agent.llm.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.plexpt.chatgpt.ChatGPT;
import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
import com.plexpt.chatgpt.entity.chat.Message;
import com.plexpt.chatgpt.entity.chat.ResponseFormat;
import com.plexpt.chatgpt.entity.embedding.EmbeddingResult;
import com.ytx.ai.agent.llm.service.LlmService;
import com.ytx.ai.agent.llm.vo.LlmChatCompletion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ChatGptService implements LlmService {

    public static final String JSON_OBJECT="json_object";

    @Autowired
    private ChatGPT chatGPT;

    @Override
    public ChatCompletionResponse chatCompletion(ChatCompletion chatCompletion) {
        return chatGPT.chatCompletion(chatCompletion);
    }

    public List<BigDecimal> createEmbeddings(String content){
        EmbeddingResult embeddingResult= chatGPT.createEmbeddings(content,null);
        System.out.println(embeddingResult);
        return null;
    }


    public String chatCompletion(LlmChatCompletion llmChatCompletion){
        List<Message> messages=new ArrayList<>();
        messages.add(Message.ofSystem(llmChatCompletion.getSystemPrompt()));
        if(ObjectUtil.isNotEmpty(llmChatCompletion.getHistory())){
            messages.addAll(llmChatCompletion.getHistory());
        }
        messages.add(Message.of(llmChatCompletion.getUserPrompt().toString()));

        ChatCompletion chatCompletion=ChatCompletion.builder()
                .messages(messages)
                .temperature(llmChatCompletion.getTemperature())
                .build();
        if(ResponseFormat.Type.JSON_OBJECT.getValue().equalsIgnoreCase(llmChatCompletion.getResponseFormat())){
            chatCompletion.setResponseFormat(ResponseFormat.builder().type(JSON_OBJECT).build());
        }
        ChatCompletionResponse response= chatGPT.chatCompletion(chatCompletion);
        if(ObjectUtil.isNull(response))
        {
            log.error("run chatCompletion failed.");
            return null;
        }
        return response.getChoices().get(0).getMessage().getContent();
    }
}
