package com.ytx.ai.base.agent;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PluginDefinition {

    private String pluginType;
    private String url;
    private String intro;
    private List<HeaderDefinition> headers;
    private String authMethod;
}