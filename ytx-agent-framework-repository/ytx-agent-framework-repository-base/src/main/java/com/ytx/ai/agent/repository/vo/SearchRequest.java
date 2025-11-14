package com.ytx.ai.agent.repository.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SearchRequest {
    private String[] labels;
    private List<Filter> filters;
    private int limit = 5;
    private Class<?> clazz;
    private String[] returnFields;
    private String sort;
}

