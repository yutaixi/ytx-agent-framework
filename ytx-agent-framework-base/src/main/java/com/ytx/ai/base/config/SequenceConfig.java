package com.ytx.ai.base.config;

import com.ytx.ai.base.service.Sequence;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SequenceConfig {

    @Bean
    public Sequence sequence()
    {
        return new Sequence();
    }

}
