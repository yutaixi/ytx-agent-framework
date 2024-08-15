package com.ytx.ai.agent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "ai.agent.main")
@Getter
@Setter
public class AgentProperty {

    boolean memoryCacheEnable=true;
    private Integer memoryCacheTime=3600;

    String replyAgentDesc="reply to user based on the result of other agents.";

    boolean enableChatToAgent=true;
}
