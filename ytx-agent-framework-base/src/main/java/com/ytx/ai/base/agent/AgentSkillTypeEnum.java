package com.ytx.ai.base.agent;

public enum AgentSkillTypeEnum {

    PLUGIN("plugin"),
    WORKFLOW("workflow");

    private String type;

    private AgentSkillTypeEnum(String type){
        this.type=type;
    }
}
