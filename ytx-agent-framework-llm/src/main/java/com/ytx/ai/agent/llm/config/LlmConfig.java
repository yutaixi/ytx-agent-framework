package com.ytx.ai.agent.llm.config;

import com.plexpt.chatgpt.ChatGPT;
import com.ytx.ai.agent.llm.service.LlmService;
import com.ytx.ai.agent.llm.service.impl.ChatGptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class LlmConfig {

    @Autowired
    private LlmConfigProperty llmConfigProperty;

    @Bean
    public ChatGPT chatGPT()
    {
        return ChatGPT.builder()
                .apiKey(llmConfigProperty.getApiKey())
                .timeout(llmConfigProperty.getTimeout())
                .apiHost(llmConfigProperty.getApiHost())
                .build()
                .init();
    }

    @Bean
    public LlmService llmService()
    {
        return new ChatGptService();
    }
}
