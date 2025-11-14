package com.ytx.ai.agent.repository.builder;

import com.ytx.ai.agent.repository.config.PgRepositoryConfig;
import com.ytx.ai.agent.repository.condition.ConditionHandler;
import com.ytx.ai.agent.repository.condition.ConditionHandlerRegistry;
import com.ytx.ai.agent.repository.exception.PgDeleteException;
import com.ytx.ai.agent.repository.exception.PgInsertException;
import com.ytx.ai.agent.repository.util.PgIdentifierEscaper;
import com.ytx.ai.agent.repository.vo.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PostgreSQL SQL 构建器
 */
@Component
public class PgSqlBuilder {

    private final PgRepositoryConfig config;
    private final PgIdentifierEscaper escaper;
    private final ConditionHandlerRegistry conditionHandlerRegistry;

    @Autowired
    public PgSqlBuilder(PgRepositoryConfig config,
                       PgIdentifierEscaper escaper,
                       ConditionHandlerRegistry conditionHandlerRegistry) {
        this.config = config;
        this.escaper = escaper;
        this.conditionHandlerRegistry = conditionHandlerRegistry;
    }

    /**
     * 构建 INSERT SQL
     */
    public String buildInsertSql(String table, Set<String> columns, boolean nodup, boolean hasBid) {
        String cols = String.join(", ", columns);
        String namedCols = columns.stream()
                .map(c -> ":" + c)
                .collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(escaper.escape(table))
                .append(" (").append(cols).append(") ")
                .append("VALUES (").append(namedCols).append(") ");

        if (nodup) {
            if (!hasBid) {
                throw new PgInsertException("nodup insert requires unique business id field '"
                        + config.getBusinessIdField() + "'.");
            }
            List<String> updateCols = columns.stream()
                    .filter(c -> !config.getBusinessIdField().equalsIgnoreCase(c)
                            && !config.getPrimaryKeyField().equalsIgnoreCase(c))
                    .collect(Collectors.toList());

            if (updateCols.isEmpty()) {
                sb.append("ON CONFLICT (").append(config.getBusinessIdField())
                        .append(") DO NOTHING");
            } else {
                String updateClause = updateCols.stream()
                        .map(c -> c + " = EXCLUDED." + c)
                        .collect(Collectors.joining(", "));
                sb.append("ON CONFLICT (").append(config.getBusinessIdField())
                        .append(") DO UPDATE SET ").append(updateClause);
            }
        }
        return sb.toString();
    }

    /**
     * 构建 WHERE 子句
     * @param filters 过滤器列表
     * @param params SQL 参数源
     * @param allowEmpty 是否允许空条件（true 用于 search，false 用于 delete）
     * @return WHERE 子句（带 WHERE 前缀）或空字符串
     */
    public String buildWhereClause(List<Filter> filters, MapSqlParameterSource params, boolean allowEmpty) {
        if (filters == null || filters.isEmpty()) {
            if (allowEmpty) {
                return "";
            }
            throw new PgDeleteException("delete node: missing filter fields.");
        }

        List<String> andParts = new ArrayList<>();
        List<String> orParts = new ArrayList<>();
        int idx = 0;

        for (Filter f : filters) {
            String cond = buildCondition(f, params, idx++);
            if (cond == null || cond.isEmpty()) {
                continue;
            }
            if (f.getLogicalOpt() != null && Filter.LogicalOpt.AND.equalsIgnoreCase(f.getLogicalOpt())) {
                andParts.add(cond);
            } else {
                orParts.add(cond);
            }
        }

        if (andParts.isEmpty() && orParts.isEmpty()) {
            if (allowEmpty) {
                return "";
            }
            throw new PgDeleteException("delete node: invalid filters lead to empty conditions.");
        }

        if (!andParts.isEmpty()) {
            // 对齐 ES 语义：存在 AND（must）时忽略 OR（should，默认可选）
            String where = andParts.stream()
                    .map(p -> "(" + p + ")")
                    .collect(Collectors.joining(" AND "));
            return "WHERE " + where;
        } else {
            String where = orParts.stream()
                    .map(p -> "(" + p + ")")
                    .collect(Collectors.joining(" OR "));
            return "WHERE " + where;
        }
    }

    /**
     * 构建单个条件
     */
    private String buildCondition(Filter filter, MapSqlParameterSource params, int idx) {
        if (filter == null || filter.getField() == null || filter.getComparisonOpt() == null) {
            return "";
        }

        ConditionHandler handler = conditionHandlerRegistry.getHandler(filter.getComparisonOpt());
        if (handler == null) {
            return "";
        }

        return handler.buildCondition(filter, params, "p" + idx);
    }

    /**
     * 构建 SELECT 字段列表
     */
    public String buildSelectFields(String[] returnFields) {
        if (returnFields == null || returnFields.length == 0) {
            return "*";
        }
        return Arrays.stream(returnFields)
                .map(escaper::escape)
                .collect(Collectors.joining(", "));
    }

    /**
     * 构建 ORDER BY 子句
     */
    public String buildOrderByClause(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return "";
        }
        // 简单处理：假设 sort 格式为 "field" 或 "field ASC/DESC"
        String[] parts = sort.trim().split("\\s+");
        if (parts.length == 0) {
            return "";
        }
        String field = escaper.escape(parts[0]);
        String direction = parts.length > 1 && "DESC".equalsIgnoreCase(parts[1]) ? "DESC" : "ASC";
        return "ORDER BY " + field + " " + direction;
    }

    /**
     * 转义标识符（委托给 escaper）
     */
    public String escapeIdentifier(String identifier) {
        return escaper.escape(identifier);
    }
}

