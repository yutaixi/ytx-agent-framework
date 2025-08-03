package com.ytx.ai.base.agent;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Skill {

    private String id;
    private AgentSkillTypeEnum type;
    private String name;
    private String description;
    private String arguments;
}
