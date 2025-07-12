package com.ytx.ai.base.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
public class RedisConfig  extends CachingConfigurerSupport {

    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory){
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        JSON2JsonRedisSerializer serializer = new JSON2JsonRedisSerializer(Object.class);
        // 添加字符串专用序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();


        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(mapper);

        // 使用StringRedisSerializer序列化和反序列化redis的key值
        template.setKeySerializer(stringSerializer);
        // 使用StringRedisSerializer序列化和反序列化redis hash类型的key值
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        // 序列化和反序列化redis hash类型的value值
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}
