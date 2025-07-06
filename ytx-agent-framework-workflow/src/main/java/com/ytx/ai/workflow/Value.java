package com.ytx.ai.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class Value {

    private String id;
    private String name;
    private Object content;
    private String type;//string,...
    private ValueSource source;
    private Map<String, Object> schema;

    private Boolean required;
    private String description;

}