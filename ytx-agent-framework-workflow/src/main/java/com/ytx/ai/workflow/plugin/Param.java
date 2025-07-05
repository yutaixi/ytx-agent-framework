package com.ytx.ai.workflow.plugin;

import com.ytx.ai.workflow.enums.ValueTypeEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Param {
    private String name;
    private ValueTypeEnum type;
    private boolean required;
    private Object defaultValue;
}
