package com.ytx.ai.agent.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@ConfigurationProperties("llm.config")
@Configuration
public class LlmConfigProperty {

    private String apiKey;
    private int timeout;
    private String apiHost;
    String defaultModel;
    Duration connectTimeout = Duration.ofSeconds(10);
    Duration readTimeout= Duration.ofSeconds(60);
}
