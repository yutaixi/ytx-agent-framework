package com.ytx.ai.agent.repository.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

@Getter
@Setter
@Builder
public class Filter {

    private String field;
    private Object value;

    /** like, equal, gte, lte, lt */
    private String comparisonOpt;

    /** and, or */
    private String logicalOpt;

    public static Filter of(Filter filter) {
        Filter target = Filter.builder().build();
        BeanUtils.copyProperties(filter, target);
        return target;
    }

    public Filter equal() {
        this.comparisonOpt = ComparisonOpt.EQUAL;
        return this;
    }

    public Filter regexp() {
        this.comparisonOpt = ComparisonOpt.REGEXP;
        return this;
    }

    public Filter like() {
        this.comparisonOpt = ComparisonOpt.LIKE;
        return this;
    }

    public Filter gte() {
        this.comparisonOpt = ComparisonOpt.GTE;
        return this;
    }

    public Filter gt() {
        this.comparisonOpt = ComparisonOpt.GT;
        return this;
    }

    public Filter lte() {
        this.comparisonOpt = ComparisonOpt.LTE;
        return this;
    }

    public Filter lt() {
        this.comparisonOpt = ComparisonOpt.LT;
        return this;
    }

    public Filter between() {
        this.comparisonOpt = ComparisonOpt.BETWEEN;
        return this;
    }
    public Filter similar() {
        this.comparisonOpt = ComparisonOpt.SIMILAR;
        return this;
    }

    public Filter and() {
        this.logicalOpt = LogicalOpt.AND;
        return this;
    }

    public Filter or() {
        this.logicalOpt = LogicalOpt.OR;
        return this;
    }


    public static final class ComparisonOpt {
        public static final String EQUAL = "equal";
        public static final String LIKE = "like";
        public static final String REGEXP = "regexp";
        public static final String GT = "gt";
        public static final String GTE = "gte";
        public static final String LT = "lt";
        public static final String LTE = "lte";
        public static final String BETWEEN = "between";
        public static final String SIMILAR = "similar";
    }

    public static final class LogicalOpt {
        public static final String AND = "and";
        public static final String OR = "or";
    }
}
