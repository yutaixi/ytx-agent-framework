package com.ytx.ai.agent.repository.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@RefreshScope
@Data
@ConfigurationProperties("ai.knowledge.repository")
@Configuration
public class RepositoryProperties {
    private String type;
}
