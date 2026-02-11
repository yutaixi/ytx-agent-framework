package com.ytx.ai.base.agent;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ToolDefinition {

    private String path;
    private String method;
    private List<ToolParam> inputs;
    private List<ToolParam> outputs;
}