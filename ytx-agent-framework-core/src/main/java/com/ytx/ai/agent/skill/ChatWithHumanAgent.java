package com.ytx.ai.agent.skill;

import cn.hutool.core.collection.ListUtil;
import com.ytx.ai.agent.constant.CommandEnum;
import com.ytx.ai.agent.dto.ChatDTO;
import com.ytx.ai.agent.execute.AgentExecuteContext;
import com.ytx.ai.agent.vo.AgentTask;
import com.ytx.ai.agent.vo.Command;
import com.ytx.ai.agent.vo.SubAgentResponse;

public class ChatWithHumanAgent extends BaseAgent{
    @Override
    public String getName() {
        return "chat_with_human_agent";
    }

    @Override
    public String getDescription() {
        return "prefer to chat with human agent.";
    }

    @Override
    public boolean isParticipateLlmPlanning() {
        return false;
    }

    @Override
    public SubAgentResponse execute(ChatDTO chatDTO, AgentTask task, AgentExecuteContext context) {
        super.execute(chatDTO, task, context);

        Command command=Command.builder()
                .name(CommandEnum.CHAT_TO_AGENT)
                .objective("chat with human agent")
                .value("chat to agent")
                .build();
        return SubAgentResponse.builder()
                .agentName(getName())
                .result(CommandEnum.CHAT_TO_AGENT)
                .commands(ListUtil.toList(command))
                .build();
    }
}
