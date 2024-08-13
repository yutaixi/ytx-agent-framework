package com.ytx.ai.llm.service.impl;

import com.plexpt.chatgpt.ChatGPT;
import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
import com.ytx.ai.llm.service.LlmService;
import org.springframework.beans.factory.annotation.Autowired;

public class ChatGptService implements LlmService {


    @Autowired
    private ChatGPT chatGPT;

    @Override
    public ChatCompletionResponse chatCompletion(ChatCompletion chatCompletion) {
        return chatGPT.chatCompletion(chatCompletion);
    }
}
