package com.ytx.ai.agent.repository.condition;

import com.ytx.ai.agent.repository.vo.Filter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * 条件处理器接口（策略模式）
 */
public interface ConditionHandler {

    /**
     * 构建 SQL 条件
     * @param filter 过滤器
     * @param params SQL 参数源
     * @param paramBase 参数名基础（如 "p0"）
     * @return SQL 条件字符串
     */
    String buildCondition(Filter filter, MapSqlParameterSource params, String paramBase);
}

