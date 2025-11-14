package com.ytx.ai.agent.repository.condition;

import com.ytx.ai.agent.repository.util.PgIdentifierEscaper;
import com.ytx.ai.agent.repository.vo.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * LIKE 条件处理器
 */
@Component
public class LikeConditionHandler implements ConditionHandler {

    private final PgIdentifierEscaper escaper;

    @Autowired
    public LikeConditionHandler(PgIdentifierEscaper escaper) {
        this.escaper = escaper;
    }

    @Override
    public String buildCondition(Filter filter, MapSqlParameterSource params, String paramBase) {
        String field = escaper.escape(filter.getField());
        String paramLike = paramBase;
        params.addValue(paramLike, "%" + String.valueOf(filter.getValue()) + "%");
        // 使用 ILIKE 做不区分大小写匹配
        return field + " ILIKE :" + paramLike;
    }
}

