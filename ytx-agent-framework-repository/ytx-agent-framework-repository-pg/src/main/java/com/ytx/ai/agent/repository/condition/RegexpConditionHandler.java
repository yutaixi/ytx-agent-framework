package com.ytx.ai.agent.repository.condition;

import com.ytx.ai.agent.repository.util.PgIdentifierEscaper;
import com.ytx.ai.agent.repository.vo.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * 正则表达式条件处理器
 */
@Component
public class RegexpConditionHandler implements ConditionHandler {

    private final PgIdentifierEscaper escaper;

    @Autowired
    public RegexpConditionHandler(PgIdentifierEscaper escaper) {
        this.escaper = escaper;
    }

    @Override
    public String buildCondition(Filter filter, MapSqlParameterSource params, String paramBase) {
        String field = escaper.escape(filter.getField());
        String paramReg = paramBase;
        params.addValue(paramReg, filter.getValue());
        // PostgreSQL 正则匹配运算符 ~ (区分大小写) 或 ~*（不区分大小写），这里用 ~
        return field + " ~ :" + paramReg;
    }
}

