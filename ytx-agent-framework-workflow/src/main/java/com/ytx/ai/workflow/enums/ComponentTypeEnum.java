package com.ytx.ai.workflow.enums;


public enum ComponentTypeEnum {

    plugin("plugin"),
    workflow("workflow");

    private final String type;

    private ComponentTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
