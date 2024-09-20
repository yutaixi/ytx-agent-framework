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

    private Integer id;
    private String name;
    private String code;
    private  int version;
    private  List<AgentRefIntentEntity> refIntentions;
    private List<AgentRefSkillEntity> refSkills;
    private  Map<String,String> agentProperties;

}
