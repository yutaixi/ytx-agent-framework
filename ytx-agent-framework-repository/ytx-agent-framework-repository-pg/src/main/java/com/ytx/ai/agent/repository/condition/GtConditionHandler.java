package com.ytx.ai.agent.repository.condition;

import com.ytx.ai.agent.repository.util.PgIdentifierEscaper;
import com.ytx.ai.agent.repository.vo.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * 大于条件处理器
 */
@Component
public class GtConditionHandler implements ConditionHandler {

    private final PgIdentifierEscaper escaper;

    @Autowired
    public GtConditionHandler(PgIdentifierEscaper escaper) {
        this.escaper = escaper;
    }

    @Override
    public String buildCondition(Filter filter, MapSqlParameterSource params, String paramBase) {
        String field = escaper.escape(filter.getField());
        params.addValue(paramBase, filter.getValue());
        return field + " > :" + paramBase;
    }
}

