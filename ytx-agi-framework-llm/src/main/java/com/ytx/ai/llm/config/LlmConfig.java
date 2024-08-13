package com.ytx.ai.llm.config;

import com.plexpt.chatgpt.ChatGPT;
import com.ytx.ai.llm.service.LlmService;
import com.ytx.ai.llm.service.impl.ChatGptService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfig {

    @Bean
    public ChatGPT chatGPT()
    {
        return ChatGPT.builder()
                .apiKey("model-name-router-7c7aa4a3549f12")
                .timeout(900)
                .apiHost("http://192.168.88.129:8090/")
                .build()
                .init();
    }


    @Bean
    public LlmService llmService()
    {
        return new ChatGptService();
    }

}
