package com.ytx.ai.workflow.config;

import com.ytx.ai.workflow.execute.AgentTaskExecutor;
import com.ytx.ai.workflow.execute.FlowExecutor;
import com.ytx.ai.workflow.plugin.agent.*;
import com.ytx.ai.workflow.plugin.flow.*;
import com.ytx.ai.workflow.service.AgentChatService;
import com.ytx.ai.workflow.service.AgentMemoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkflowConfig {

    @Bean
    public FlowExecutor flowExecutor() {
        return new FlowExecutor();
    }

    @Bean
    public AgentTaskExecutor agentTaskExecutor() {
        return new AgentTaskExecutor();
    }

    @Bean
    public AgentMemoryService agentMemoryService() {
        return new AgentMemoryService();
    }

    @Bean
    public AgentChatService agentChatService() {
        return new AgentChatService();
    }

    @Bean
    public CodePlugin codePlugin() {
        return new CodePlugin();
    }
//
    @Bean
    public ConditionPlugin conditionPlugin() {
        return new ConditionPlugin();
    }

    @Bean
    public HttpPlugin httpPlugin() {
        return new HttpPlugin();
    }

    @Bean
    public LlmPlugin llmPlugin() {
        return new LlmPlugin();
    }

    @Bean
    public FlowStart flowStart() {
        return new FlowStart();
    }

    @Bean
    public FlowEnd flowEnd() {
        return new FlowEnd();
    }


    @Bean
    public SubProcessPlugin subProcessPlugin() {
        return new SubProcessPlugin();
    }



    @Bean
    public AgentStartPlugin agentStartPlugin() {
        return new AgentStartPlugin();
    }

    @Bean
    public AgentIntentPlugin agentIntentPlugin() {
        return new AgentIntentPlugin();
    }

    @Bean
    public AgentPlanPlugin agentPlanPlugin() {
        return new AgentPlanPlugin();
    }

    @Bean
    public AgentExecutorPlugin agentExecutorPlugin() {
        return new AgentExecutorPlugin();
    }

    @Bean
    public AgentReplyPlugin agentReplyPlugin() {
        return new AgentReplyPlugin();
    }

    @Bean
    public AgentEndPlugin agentEndPlugin() {
        return new AgentEndPlugin();
    }


}
