package com.ytx.ai.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ytx.ai.agent.entity.SkillEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SkillMapper extends BaseMapper<SkillEntity> {

    public List<SkillEntity> queryAll();

    public SkillEntity find(@Param("skillId") Integer skillId);

}
