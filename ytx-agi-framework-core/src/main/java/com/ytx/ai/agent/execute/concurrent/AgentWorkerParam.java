package com.ytx.ai.agent.execute.concurrent;

import com.ytx.ai.agent.dto.ChatDTO;
import com.ytx.ai.agent.execute.AgentExecuteContext;
import com.ytx.ai.agent.skill.AiAgent;
import com.ytx.ai.agent.vo.AgentTask;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AgentWorkerParam {

    AiAgent agent;
    AgentTask targetTask;
    ChatDTO chatDTO;
    AgentExecuteContext agentExecuteContext;

}
