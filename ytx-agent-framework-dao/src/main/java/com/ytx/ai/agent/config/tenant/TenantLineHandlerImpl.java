package com.ytx.ai.agent.config.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.ytx.ai.base.tenant.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;

import java.util.Set;

/**
 * MyBatis-Plus 多租户 SQL 拦截处理器
 * 负责向 SQL 自动注入 tenant_code 过滤条件，实现行级数据隔离。
 *
 * 工作原理：
 *   - getTenantId()：返回当前请求的租户 code，由 MyBatis-Plus 拼入 WHERE 子句
 *   - ignoreTable()：控制哪些表不参与租户过滤（公共表、关联表）
 *   - 若当前请求无租户上下文（tenantCode == null），则对所有表跳过过滤
 *
 * 需要租户过滤的表：ai_agent、ai_skill、ai_skill_tool
 * 跳过租户过滤的表（IGNORED_TABLES）：
 *   - ai_plugin_store：插件市场，全平台共享
 *   - ai_intention：系统意图，全局配置
 *   - ai_agent_ref_intent：Agent-意图关联表，通过 agentId 过滤
 *   - ai_agent_ref_tool：Agent-工具关联表，通过 agentId 过滤
 *   - ai_agent_property：Agent 属性表，通过 agentId 过滤
 */
public class TenantLineHandlerImpl implements TenantLineHandler {

    /** 不参与租户过滤的表名集合（小写匹配） */
    private static final Set<String> IGNORED_TABLES = Set.of(
            "ai_plugin_store",
            "ai_intention",
            "ai_agent_ref_intent",
            "ai_agent_ref_tool",
            "ai_agent_property"
    );

    /**
     * 返回当前租户的 SQL 表达式，插件会将此值拼入 WHERE tenant_code = ? 条件
     * @return 租户 code 的 SQL 字符串表达式；无上下文时返回空串（配合 ignoreTable 不会被实际使用）
     */
    @Override
    public Expression getTenantId() {
        String tenantCode = TenantContextHolder.getTenantCode();
        return new StringValue(tenantCode != null ? tenantCode : "");
    }

    /**
     * 租户字段列名，对应数据库中存储租户标识的列
     * @return 列名 "tenant_code"
     */
    @Override
    public String getTenantIdColumn() {
        return "tenant_code";
    }

    /**
     * 判断指定表是否跳过租户过滤
     * @param tableName 数据库表名
     * @return true 表示跳过（不注入 tenant_code 条件），false 表示需要过滤
     *         若当前请求无租户上下文，所有表均跳过，保证系统内部调用正常工作
     */
    @Override
    public boolean ignoreTable(String tableName) {
        // 无租户上下文时（如系统内部调用），跳过所有表的租户过滤
        if (TenantContextHolder.getTenantCode() == null) {
            return true;
        }
        // 按白名单跳过公共表/关联表
        return IGNORED_TABLES.contains(tableName.toLowerCase());
    }
}

