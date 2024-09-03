package com.ytx.ai.agent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "ai.agent.executor")
@Getter
@Setter
public class TaskExecutorProperty {

    String taskNotExecuteResult="not executed yet due to the necessary tasks have not been successfully executed yet.";

    private long asyncWorkerTimeout=30000;
}
