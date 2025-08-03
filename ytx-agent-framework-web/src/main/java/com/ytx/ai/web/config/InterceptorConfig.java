package com.ytx.ai.web.config;

import com.ytx.ai.web.Interceptor.RequestContextInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Bean
    public RequestContextInterceptor requestContextInterceptor()
    {
        return new RequestContextInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
         registry.addInterceptor(requestContextInterceptor());
    }
}
