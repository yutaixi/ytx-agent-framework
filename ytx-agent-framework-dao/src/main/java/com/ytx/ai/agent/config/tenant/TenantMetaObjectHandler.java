package com.ytx.ai.agent.config.tenant;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.ytx.ai.base.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

/**
 * MyBatis-Plus 字段自动填充处理器（多租户）
 * 在执行 INSERT 操作时，自动将当前请求的租户 code 填充到实体的 tenantCode 字段，
 * 前提是实体的 tenantCode 字段标注了 @TableField(fill = FieldFill.INSERT)。
 *
 * 适用实体：AgentEntity、SkillEntity、SkillToolEntity
 *
 * 填充逻辑：
 *   - 若 TenantContextHolder 中有租户上下文，则填充该值
 *   - 若无租户上下文（系统内部调用），则不填充，保留实体中原有值（可能为 null）
 */
@Slf4j
public class TenantMetaObjectHandler implements MetaObjectHandler {

    /** 租户字段的 Java 属性名（驼峰命名，对应数据库列 tenant_code） */
    private static final String TENANT_CODE_FIELD = "tenantCode";

    /**
     * INSERT 时自动填充：将当前请求的租户 code 写入实体的 tenantCode 字段
     * @param metaObject MyBatis 反射元对象，通过它访问实体字段
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 仅在实体包含 tenantCode 字段时才填充，避免对不支持租户的实体报错
        if (!metaObject.hasSetter(TENANT_CODE_FIELD)) {
            return;
        }
        String tenantCode = TenantContextHolder.getTenantCode();
        if (tenantCode != null) {
            this.strictInsertFill(metaObject, TENANT_CODE_FIELD, String.class, tenantCode);
        }
    }

    /**
     * UPDATE 时自动填充：租户 code 不在更新时修改，此方法留空
     * @param metaObject MyBatis 反射元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 租户归属不允许在更新时变更
    }
}