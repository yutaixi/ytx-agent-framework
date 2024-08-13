package com.ytx.ai.agent.skill;

public interface AiAgent {

    public String getName();

    public String getDescription();

    default public boolean isParticipateLlmPlanning(){
        return true;
    }

}
