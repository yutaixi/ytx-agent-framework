package com.ytx.ai.agent.repository.condition;

import com.ytx.ai.agent.repository.vo.Filter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * 相似度搜索条件处理器
 * 向量相似度搜索：返回总是为真的条件，实际排序在 search 方法中处理
 */
@Component
public class SimilarConditionHandler implements ConditionHandler {

    @Override
    public String buildCondition(Filter filter, MapSqlParameterSource params, String paramBase) {
        // 向量相似度搜索：返回总是为真的条件，实际排序在 search 方法中处理
        // 向量值会在 search 方法中使用，这里只返回一个占位条件
        return "1=1";
    }
}

