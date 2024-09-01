package com.ytx.ai.agent.tool;

import com.ytx.ai.agent.skill.BaseAgent;

public abstract class BaseTool extends BaseAgent {

    @Override
    public boolean isParticipateLlmPlanning() {
        return false;
    }
}
