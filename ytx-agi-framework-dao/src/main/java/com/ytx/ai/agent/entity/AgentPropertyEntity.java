package com.ytx.ai.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@TableName("ai_agent_property")
@Getter
@Setter
public class AgentPropertyEntity {
    Integer id;
    int agentId;
    String propertyName;
    String propertyValue;
    String propertyGroup;
}
