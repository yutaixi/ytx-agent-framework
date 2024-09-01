package com.ytx.ai.agent.vo;


import com.ytx.ai.agent.entity.AgentRefIntentEntity;
import com.ytx.ai.agent.entity.AgentRefSkillEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AgentInfo {

    Integer id;
    String name;
    String code;
    int version;
    List<AgentRefIntentEntity> refIntentions;
    List<AgentRefSkillEntity> refSkills;
    Map<String,String> agentProperties;

}
