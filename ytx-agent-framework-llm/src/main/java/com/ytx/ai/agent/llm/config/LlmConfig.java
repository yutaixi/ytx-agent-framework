package com.ytx.ai.agent.llm.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.ytx.ai.agent.llm.service.LlmService;
import com.ytx.ai.agent.llm.service.impl.ChatGptService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfig {

    @Bean
    public OpenAIClient openAIClient(LlmConfigProperty llmConfigProperty)
    {
        return OpenAIOkHttpClient.builder()
                .apiKey(llmConfigProperty.getApiKey())
                .baseUrl(llmConfigProperty.getApiHost())
                .timeout(llmConfigProperty.getReadTimeout())
                .build();
    }

    @Bean
    public LlmService llmService()
    {
        return new ChatGptService();
    }
}