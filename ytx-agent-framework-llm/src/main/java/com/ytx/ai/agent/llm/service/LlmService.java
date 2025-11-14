package com.ytx.ai.agent.llm.service;

import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
import com.ytx.ai.agent.llm.vo.LlmChatCompletion;

import java.util.List;

public interface LlmService {

    public ChatCompletionResponse chatCompletion(ChatCompletion chatCompletion);

    public String chatCompletion(LlmChatCompletion llmChatCompletion);


    public List<Float> createEmbeddings(String content);
}
