package com.ytx.ai.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@TableName("ai_agent_ref_intent")
@Getter
@Setter
public class AgentRefIntentEntity {

    private Integer id;
    private int agentId;
    private int intentId;
}
