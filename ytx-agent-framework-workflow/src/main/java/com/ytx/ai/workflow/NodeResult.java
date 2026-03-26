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
    /** 节点执行失败时的错误信息，正常执行时为 null */
    private String errorMessage;
}