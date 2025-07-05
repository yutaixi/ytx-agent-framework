package com.ytx.ai.agent.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@RefreshScope
@Data
@ConfigurationProperties("llm.config")
@Configuration
public class LlmConfigProperty {

    private String apiKey;
    private int timeout;
    private String apiHost;
}
