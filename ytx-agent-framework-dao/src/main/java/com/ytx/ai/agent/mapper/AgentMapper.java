package com.ytx.ai.agent.mapper;

import com.ytx.ai.agent.entity.AgentEntity;
import org.apache.ibatis.annotations.Param;

public interface AgentMapper {

    public AgentEntity findAgent(@Param("agentCode") String agentCode);

    public int getVersion(@Param("agentCode") String agentCode);

}
