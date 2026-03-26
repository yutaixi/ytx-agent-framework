package com.ytx.ai.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 技能工具实体，对应数据库表 ai_skill_tool
 * 支持多租户：每条记录通过 tenant_code 标识所属租户，
 * tenant_code 在新增时由 TenantMetaObjectHandler 自动填充。
 */
@TableName(value = "ai_skill_tool")
@Getter
@Setter
public class SkillToolEntity {

    @TableId(type = IdType.AUTO)
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

    /**
     * 租户标识码，对应 x-header-tenant 请求头传入的值。
     * 新增时由 TenantMetaObjectHandler 自动从 TenantContextHolder 中读取并填充。
     */
    @TableField(fill = FieldFill.INSERT)
    private String tenantCode;
}