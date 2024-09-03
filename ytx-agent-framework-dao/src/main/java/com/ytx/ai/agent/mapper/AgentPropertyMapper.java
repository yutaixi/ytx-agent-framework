package com.ytx.ai.agent.mapper;

import com.ytx.ai.agent.entity.AgentPropertyEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AgentPropertyMapper {

    public List<AgentPropertyEntity> findAgentProperties(@Param("agentId") int agentId);

}
