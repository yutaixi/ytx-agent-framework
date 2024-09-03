package com.ytx.ai.agent.mapper;

import com.ytx.ai.agent.entity.AgentRefSkillEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AgentRefToolMapper {

    public List<AgentRefSkillEntity> findAgentRefSkills(@Param("agentId") int agentId);

}
