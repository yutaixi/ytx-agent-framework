package com.ytx.ai.agent.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@TableName("ai_agent_ref_tool")
@Getter
@Setter
public class AgentRefSkillEntity {
    Integer id;
    int agentId;
    int skillId;
    String name;
}
