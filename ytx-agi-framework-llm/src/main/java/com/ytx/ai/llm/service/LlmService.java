package com.ytx.ai.llm.service;

import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;

public interface LlmService {

    public ChatCompletionResponse chatCompletion(ChatCompletion chatCompletion);
}
