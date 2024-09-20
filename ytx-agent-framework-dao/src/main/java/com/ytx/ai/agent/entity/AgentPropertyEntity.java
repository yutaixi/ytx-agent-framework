package com.ytx.ai.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@TableName("ai_agent_property")
@Getter
@Setter
public class AgentPropertyEntity {
    private Integer id;
    private int agentId;
    private String propertyName;
    private String propertyValue;
    private String propertyGroup;
}
