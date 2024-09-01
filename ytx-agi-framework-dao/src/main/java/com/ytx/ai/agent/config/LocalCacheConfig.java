package com.ytx.ai.agent.config;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class LocalCacheConfig {

    @Bean("caffeineCache")
    public Cache<String,Object> caffeineCache(){
        return Caffeine.newBuilder()
                .initialCapacity(1000)
                .maximumSize(9999)
                .expireAfterWrite(7, TimeUnit.DAYS)
                .removalListener((key,value,cause)->{
                    log.info("caffeineCache 键：{}，值：{}，被清除了，清除原因是：{}",key,value,cause);
                })
                .build();
    }

}
