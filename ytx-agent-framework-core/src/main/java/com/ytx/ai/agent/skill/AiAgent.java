//package com.ytx.ai.agent.skill;
//
//import com.ytx.ai.base.agent.ChatDTO;
//import com.ytx.ai.workflow.execute.AgentExecuteContext;
//import com.ytx.ai.base.agent.AgentTask;
//import com.ytx.ai.base.agent.SubAgentResponse;
//
//public interface AiAgent {
//
//    public String getName();
//
//    public String getDescription();
//
//    default public boolean isParticipateLlmPlanning(){
//        return true;
//    }
//
//    default SubAgentResponse execute(ChatDTO chatDTO, AgentTask task, AgentExecuteContext context){
//        System.out.println("run agent "+getName());
//        return null;
//    }
//
//}
