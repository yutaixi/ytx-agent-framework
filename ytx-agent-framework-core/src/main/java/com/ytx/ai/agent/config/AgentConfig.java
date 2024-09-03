package com.ytx.ai.agent.config;

import com.ytx.ai.agent.*;
import com.ytx.ai.agent.execute.AgentTaskExecutor;
import com.ytx.ai.agent.service.AgentMemoryService;
import com.ytx.ai.agent.service.IntentionTaskService;
import com.ytx.ai.agent.skill.ChatWithHumanAgent;
import com.ytx.ai.agent.skill.GetUserLanguageAgent;
import com.ytx.ai.agent.skill.ReplyToUserAgent;
import com.ytx.ai.agent.tool.CrisisIdentifyTool;
import com.ytx.ai.agent.tool.IntentionIdentifyTool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    @Bean
    public SkillRegister skillRegister(){
        return new SkillRegister();
    }

    @Bean
    public IntentionTaskService intentionTaskService(){
        return new IntentionTaskService();
    }

    @Bean
    public AgentMemoryService agentMemoryService()
    {
        return new AgentMemoryService();
    }

    @Bean
    public AgentTaskExecutor agentTaskExecutor(){
        return new AgentTaskExecutor();
    }


    @Bean
    public CrisisIdentifyTool crisisIdentifyTool(){
        return new CrisisIdentifyTool();
    }
    @Bean
    public IntentionIdentifyTool intentionIdentifyTool(){
        return new IntentionIdentifyTool();
    }
    @Bean
    public IntentionAgent intentionAgent(){
        return new IntentionAgent();
    }

    @Bean
    public IntentionRegister intentionRegister(){
        return new IntentionRegister();
    }

    @Bean
    public PlanAgent planAgent(){
        return new PlanAgent();
    }

    @Bean
    public MasterAgent masterAgent(){
        return new MasterAgent();
    }

    @Bean
    public ReplyToUserAgent replyToUserAgent(){
        return new ReplyToUserAgent();
    }

    @Bean
    public ChatWithHumanAgent chatWithHumanAgent(){
        return new ChatWithHumanAgent();
    }

    @Bean
    public  AgentContextAware agentContextAware(){
        return new AgentContextAware();
    }

    @Bean
    public GetUserLanguageAgent getUserLanguageAgent(){
        return new GetUserLanguageAgent();
    }

    @Bean
    @ConditionalOnMissingBean(value = AgentHolder.class)
    public AgentHolder agentHolder(){
        return new DefaultAgentHolder();
    }
}
