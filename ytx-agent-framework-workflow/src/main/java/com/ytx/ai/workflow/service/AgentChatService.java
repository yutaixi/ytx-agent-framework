package com.ytx.ai.workflow.service;

import com.ytx.ai.agent.entity.AgentEntity;
import com.ytx.ai.agent.service.AgentService;
import com.ytx.ai.base.agent.AgentResponse;
import com.ytx.ai.base.agent.ChatDTO;
import com.ytx.ai.web.Interceptor.RequestContext;
import com.ytx.ai.workflow.Workflow;
import com.ytx.ai.workflow.WorkflowOutput;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.execute.FlowExecutor;
import org.springframework.beans.factory.annotation.Autowired;

public class AgentChatService {
    @Autowired
    private AgentService agentService;
    @Autowired
    private FlowExecutor flowExecutor;


    public AgentResponse run(ChatDTO chatDTO){

        String agentCode= RequestContext.getAgentCode();

        AgentEntity agentEntity=agentService.getAgentInfo(agentCode);
        Workflow workflow=Workflow.of(agentEntity);
        FlowContext context = FlowContext.of(chatDTO);
        WorkflowOutput workflowOutput = flowExecutor.execute(workflow, context);
        System.out.println(workflowOutput.getAnswer());

        return AgentResponse.builder()
                .result(workflowOutput.getAnswer())
                .build();
    }
}
