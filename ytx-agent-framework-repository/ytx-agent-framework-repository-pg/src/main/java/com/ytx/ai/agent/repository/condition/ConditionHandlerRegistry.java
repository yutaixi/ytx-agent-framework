package com.ytx.ai.agent.repository.condition;

import com.ytx.ai.agent.repository.vo.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 条件处理器注册表
 */
@Component
public class ConditionHandlerRegistry {

    private final Map<String, ConditionHandler> handlers = new HashMap<>();

    @Autowired
    private EqualConditionHandler equalConditionHandler;

    @Autowired
    private LikeConditionHandler likeConditionHandler;

    @Autowired
    private RegexpConditionHandler regexpConditionHandler;

    @Autowired
    private GtConditionHandler gtConditionHandler;

    @Autowired
    private GteConditionHandler gteConditionHandler;

    @Autowired
    private LtConditionHandler ltConditionHandler;

    @Autowired
    private LteConditionHandler lteConditionHandler;

    @Autowired
    private BetweenConditionHandler betweenConditionHandler;

    @Autowired
    private SimilarConditionHandler similarConditionHandler;

    @PostConstruct
    public void init() {
        register(Filter.ComparisonOpt.EQUAL, equalConditionHandler);
        register(Filter.ComparisonOpt.LIKE, likeConditionHandler);
        register(Filter.ComparisonOpt.REGEXP, regexpConditionHandler);
        register(Filter.ComparisonOpt.GT, gtConditionHandler);
        register(Filter.ComparisonOpt.GTE, gteConditionHandler);
        register(Filter.ComparisonOpt.LT, ltConditionHandler);
        register(Filter.ComparisonOpt.LTE, lteConditionHandler);
        register(Filter.ComparisonOpt.BETWEEN, betweenConditionHandler);
        register(Filter.ComparisonOpt.SIMILAR, similarConditionHandler);
    }

    public void register(String opt, ConditionHandler handler) {
        handlers.put(opt, handler);
    }

    public ConditionHandler getHandler(String opt) {
        return handlers.get(opt);
    }
}

