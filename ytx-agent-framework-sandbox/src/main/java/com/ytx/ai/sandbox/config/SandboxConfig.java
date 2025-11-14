package com.ytx.ai.sandbox.config;

import com.ytx.ai.sandbox.ext.HttpUtil;
import com.ytx.ai.sandbox.ext.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class SandboxConfig {

    @PostConstruct
    public void init() {
        // 设置系统属性，不打印警告信息
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
//        System.setProperty("polyglot.engine.Compilation", "true");
    }

    @Bean
    public HttpUtil httpUtil() {
        return new HttpUtil();
    }

    @Bean
    public StringUtils stringUtils() {
        return new StringUtils();
    }
}
