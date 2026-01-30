package com.ytx.ai.agent.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ytx.ai.base.workflow.Flow;
import lombok.Getter;
import lombok.Setter;


@TableName(value = "ai_skill")
@Getter
@Setter
public class SkillEntity implements Flow {

    @TableId(type = IdType.AUTO) // 主键自动生成
    private Integer id;
    private String name;
    private String description;
    private String type;
    private String icon;
    private String definition;
    private Integer ver;
}

