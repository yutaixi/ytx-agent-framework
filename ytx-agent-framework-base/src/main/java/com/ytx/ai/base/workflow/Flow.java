package com.ytx.ai.base.workflow;

public interface Flow {

    public Integer getId();
    public String getName();
    public String getDescription();
    public String getType();
    public String getDefinition();
    public Integer getVer();
}
