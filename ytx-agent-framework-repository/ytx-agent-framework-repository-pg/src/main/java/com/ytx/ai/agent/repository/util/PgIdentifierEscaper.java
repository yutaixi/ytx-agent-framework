package com.ytx.ai.agent.repository.util;

import org.springframework.stereotype.Component;

/**
 * PostgreSQL 标识符转义工具类
 */
@Component
public class PgIdentifierEscaper {

    /**
     * 转义 PostgreSQL 标识符（表名、列名等）
     * 防止 SQL 注入
     */
    public String escape(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return identifier;
        }
        // 转义双引号
        if (identifier.contains("\"")) {
            identifier = identifier.replace("\"", "\"\"");
        }
        return "\"" + identifier + "\"";
    }
}

