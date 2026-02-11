package com.ytx.ai.workflow.config;

import com.ytx.ai.workflow.execute.AgentTaskExecutor;
import com.ytx.ai.workflow.execute.FlowExecutor;
import com.ytx.ai.workflow.node.agent.*;
import com.ytx.ai.workflow.node.internal.*;
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
    public CodeNode codePlugin() {
        return new CodeNode();
    }
//
    @Bean
    public ConditionNode conditionPlugin() {
        return new ConditionNode();
    }

    @Bean
    public HttpNode httpPlugin() {
        return new HttpNode();
    }

    @Bean
    public LlmNode llmPlugin() {
        return new LlmNode();
    }


    @Bean
    public BatchNode batchNode(){
        return new BatchNode();
    }

    @Bean
    public BatchBodyNode batchBodyNode(){
        return new BatchBodyNode();
    }

    @Bean
    public PluginNode pluginNode() {
        return new PluginNode();
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
    public SubProcessNode subProcessPlugin() {
        return new SubProcessNode();
    }



    @Bean
    public AgentStartNode agentStartPlugin() {
        return new AgentStartNode();
    }

    @Bean
    public AgentIntentNode agentIntentPlugin() {
        return new AgentIntentNode();
    }

    @Bean
    public AgentPlanNode agentPlanPlugin() {
        return new AgentPlanNode();
    }

    @Bean
    public AgentExecutorNode agentExecutorPlugin() {
        return new AgentExecutorNode();
    }

    @Bean
    public AgentReplyNode agentReplyPlugin() {
        return new AgentReplyNode();
    }

    @Bean
    public AgentEndNode agentEndPlugin() {
        return new AgentEndNode();
    }


}
