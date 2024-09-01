package com.ytx.ai.agent.mapper;

import com.ytx.ai.agent.entity.AgentRefIntentEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AgentRefIntentMapper {

    public List<AgentRefIntentEntity> findAgentRefIntent(@Param("agentId") int agentId);
}
