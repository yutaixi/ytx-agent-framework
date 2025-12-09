package com.ytx.ai.agent.config;

import com.ytx.ai.agent.service.AgentService;
import com.ytx.ai.agent.service.SkillService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = {"com.ytx.ai.agent.mapper"})
public class AgentDaoConfig {

    @Bean
    public AgentService agentService()
    {
        return new AgentService();
    }

    @Bean
    public SkillService skillService(){
        return new SkillService();
    }

}
