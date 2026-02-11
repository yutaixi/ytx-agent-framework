package com.ytx.ai.workflow.service;

import com.ytx.ai.agent.entity.AgentEntity;
import com.ytx.ai.agent.llm.callback.StreamCallback;
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

    /**
     * 以流式方式执行智能体会话。
     * <p>
     * 使用说明：
     * <ul>
     *     <li>调用方需实现 {@link StreamCallback}，在实现中将增量结果推送给前端（如 SSE、WebSocket）。</li>
     *     <li>本方法不会直接返回 {@link AgentResponse}，而是通过回调实时输出结果，适用于对实时性要求更高的场景。</li>
     *     <li>最终完整回答可在 {@link StreamCallback#onCompleted(String)} 中获取。</li>
     * </ul>
     *
     * @param chatDTO  对话请求参数，包含用户输入、会话上下文等信息
     * @param callback 工作流流式回调，用于实时接收增量回答与最终结果
     */
    public AgentResponse run(ChatDTO chatDTO, StreamCallback callback){

        String agentCode= RequestContext.getAgentCode();

        AgentEntity agentEntity=agentService.getAgentInfo(agentCode);
        Workflow workflow=Workflow.of(agentEntity);
        FlowContext context = FlowContext.of(chatDTO);
        context.setStreamCallback(callback);
        WorkflowOutput workflowOutput =flowExecutor.execute(workflow, context);

        return AgentResponse.builder()
                .result(workflowOutput.getAnswer())
                .build();
    }
}