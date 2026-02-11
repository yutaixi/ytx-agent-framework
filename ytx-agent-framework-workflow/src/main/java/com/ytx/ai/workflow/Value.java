package com.ytx.ai.workflow;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
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