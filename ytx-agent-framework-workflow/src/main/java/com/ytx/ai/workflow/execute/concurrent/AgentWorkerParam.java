package com.ytx.ai.workflow.execute.concurrent;

import com.ytx.ai.agent.service.SkillService;
import com.ytx.ai.base.agent.ChatDTO;
import com.ytx.ai.workflow.execute.AgentExecuteContext;
import com.ytx.ai.base.agent.AgentTask;
import com.ytx.ai.base.agent.Skill;
import com.ytx.ai.workflow.execute.FlowExecutor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

@Getter
@Setter
@Builder
public class AgentWorkerParam {

    private Skill skill;
    private AgentTask targetTask;
    private ChatDTO chatDTO;
    private AgentExecuteContext agentExecuteContext;

    private FlowExecutor flowExecutor;
    private SkillService skillService;

}
