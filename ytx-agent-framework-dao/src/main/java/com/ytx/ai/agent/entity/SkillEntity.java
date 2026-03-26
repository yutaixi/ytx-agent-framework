package com.ytx.ai.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.ytx.ai.base.workflow.Flow;
import lombok.Getter;
import lombok.Setter;

/**
 * 技能实体，对应数据库表 ai_skill
 * 支持多租户：每条记录通过 tenant_code 标识所属租户，
 * tenant_code 在新增时由 TenantMetaObjectHandler 自动填充。
 */
@TableName(value = "ai_skill")
@Getter
@Setter
public class SkillEntity implements Flow {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String description;
    private String type;
    private String icon;
    private String definition;
    private Integer ver;

    /**
     * 租户标识码，对应 x-header-tenant 请求头传入的值。
     * 新增时由 TenantMetaObjectHandler 自动从 TenantContextHolder 中读取并填充。
     */
    @TableField(fill = FieldFill.INSERT)
    private String tenantCode;
}