package com.ytx.ai.agent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "ai.agent.intent")
@Getter
@Setter
public class IntentAgentProperty {
    Double chatTemperature=0.7;
}
