package com.ytx.ai.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ytx.ai.agent.entity.AgentEntity;
import org.apache.ibatis.annotations.Param;

public interface AgentMapper extends BaseMapper<AgentEntity> {

    public AgentEntity findAgent(@Param("agentCode") String agentCode);

    public int getVersion(@Param("agentCode") String agentCode);

}
