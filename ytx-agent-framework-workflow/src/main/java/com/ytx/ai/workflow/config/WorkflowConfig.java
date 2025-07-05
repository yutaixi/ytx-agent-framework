package com.ytx.ai.workflow.config;

import com.ytx.ai.workflow.execute.FlowExecutor;
import com.ytx.ai.workflow.plugin.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkflowConfig {

    @Bean
    public FlowExecutor flowExecutor() {
        return new FlowExecutor();
    }

//    @Bean
//    public CodePlugin codePlugin() {
//        return new CodePlugin();
//    }
//
//    @Bean
//    public ConditionPlugin conditionPlugin() {
//        return new ConditionPlugin();
//    }
//
//    @Bean
//    public HttpPlugin httpPlugin() {
//        return new HttpPlugin();
//    }

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
}
