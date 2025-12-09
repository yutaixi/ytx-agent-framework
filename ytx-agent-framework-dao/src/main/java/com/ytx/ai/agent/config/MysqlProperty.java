package com.ytx.ai.agent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.datasource.mysql")
@Getter
@Setter
public class MysqlProperty {

    private String driverClassName;
    private String url;
    private String username;
    private String password;
}
