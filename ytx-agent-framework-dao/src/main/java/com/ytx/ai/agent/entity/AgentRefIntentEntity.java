package com.ytx.ai.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@TableName("ai_agent_ref_intent")
@Getter
@Setter
public class AgentRefIntentEntity {

    Integer id;
    int agentId;
    int intentId;
}
