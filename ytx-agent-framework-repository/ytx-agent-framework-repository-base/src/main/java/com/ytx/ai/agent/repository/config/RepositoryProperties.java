package com.ytx.ai.agent.repository.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@ConfigurationProperties("ai.knowledge.repository")
@Configuration
public class RepositoryProperties {
    private String type;
}
