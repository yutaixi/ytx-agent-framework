package com.ytx.ai.agent.base.config;

import com.ytx.ai.base.cache.CacheService;
import com.ytx.ai.base.cache.RedisCacheService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheService cacheService()
    {
        return new RedisCacheService();
    }
}
