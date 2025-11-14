package com.ytx.ai.agent.repository.condition;

import com.ytx.ai.agent.repository.exception.PgRepositoryException;
import com.ytx.ai.agent.repository.util.PgIdentifierEscaper;
import com.ytx.ai.agent.repository.vo.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Iterator;

/**
 * BETWEEN 条件处理器
 */
@Component
public class BetweenConditionHandler implements ConditionHandler {

    private final PgIdentifierEscaper escaper;

    @Autowired
    public BetweenConditionHandler(PgIdentifierEscaper escaper) {
        this.escaper = escaper;
    }

    @Override
    public String buildCondition(Filter filter, MapSqlParameterSource params, String paramBase) {
        String field = escaper.escape(filter.getField());
        Object v = filter.getValue();
        Object lo = null;
        Object hi = null;

        if (v instanceof Collection) {
            Iterator<?> it = ((Collection<?>) v).iterator();
            if (it.hasNext()) {
                lo = it.next();
            }
            if (it.hasNext()) {
                hi = it.next();
            }
        } else if (v != null && v.getClass().isArray()) {
            Object[] arr = (Object[]) v;
            if (arr.length > 0) {
                lo = arr[0];
            }
            if (arr.length > 1) {
                hi = arr[1];
            }
        }

        if (lo == null || hi == null) {
            throw new PgRepositoryException("between search need two params.");
        }

        String pLo = paramBase + "_lo";
        String pHi = paramBase + "_hi";
        params.addValue(pLo, lo);
        params.addValue(pHi, hi);
        return field + " BETWEEN :" + pLo + " AND :" + pHi;
    }
}

