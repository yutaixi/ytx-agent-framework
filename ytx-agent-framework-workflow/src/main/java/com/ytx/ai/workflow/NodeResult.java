package com.ytx.ai.workflow;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
public class NodeResult {
    private Map<String, Value> data;
    private NodeMeta nodeMeta;
    private String answer;
    private boolean skip;
    private long cost;
}