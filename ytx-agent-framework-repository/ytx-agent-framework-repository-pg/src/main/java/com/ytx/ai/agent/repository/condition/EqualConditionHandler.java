package com.ytx.ai.agent.repository.condition;

import com.ytx.ai.agent.repository.util.PgIdentifierEscaper;
import com.ytx.ai.agent.repository.vo.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * 等于条件处理器
 */
@Component
public class EqualConditionHandler implements ConditionHandler {

    private final PgIdentifierEscaper escaper;

    @Autowired
    public EqualConditionHandler(PgIdentifierEscaper escaper) {
        this.escaper = escaper;
    }

    @Override
    public String buildCondition(Filter filter, MapSqlParameterSource params, String paramBase) {
        String field = escaper.escape(filter.getField());
        Object val = filter.getValue();

        if (val instanceof Collection) {
            String paramName = paramBase + "_in";
            params.addValue(paramName, val);
            return field + " IN (:" + paramName + ")";
        } else if (val != null && val.getClass().isArray()) {
            String paramName = paramBase + "_in";
            params.addValue(paramName, val);
            return field + " IN (:" + paramName + ")";
        } else {
            params.addValue(paramBase, val);
            return field + " = :" + paramBase;
        }
    }
}

