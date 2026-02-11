package com.ytx.ai.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@TableName(value = "ai_skill_tool")
@Getter
@Setter
public class SkillToolEntity {

    @TableId(type = IdType.AUTO) // 主键自动生成
    private Integer id;
    private Integer skillId;
    private String name;
    private String description;
    private String definition;
    private Integer serviceStatus;
    private Integer debugStatus;
    private Date createTime;
    private Date updateTime;
    private Boolean disabled;
}
