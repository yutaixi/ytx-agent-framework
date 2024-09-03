package com.ytx.ai.agent.skill;

import com.ytx.ai.agent.dto.ChatDTO;
import com.ytx.ai.agent.execute.AgentExecuteContext;
import com.ytx.ai.agent.vo.AgentTask;
import com.ytx.ai.agent.vo.SubAgentResponse;

public interface AiAgent {

    public String getName();

    public String getDescription();

    default public boolean isParticipateLlmPlanning(){
        return true;
    }

    default SubAgentResponse execute(ChatDTO chatDTO, AgentTask task, AgentExecuteContext context){
        System.out.println("run agent "+getName());
        return null;
    }

}
