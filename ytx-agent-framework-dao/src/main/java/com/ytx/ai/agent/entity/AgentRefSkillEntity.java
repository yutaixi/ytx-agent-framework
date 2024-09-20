package com.ytx.ai.agent.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@TableName("ai_agent_ref_tool")
@Getter
@Setter
public class AgentRefSkillEntity {
    private Integer id;
    private int agentId;
    private int skillId;
    private String name;
}
