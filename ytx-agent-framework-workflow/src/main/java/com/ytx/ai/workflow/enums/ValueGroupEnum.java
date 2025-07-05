package com.ytx.ai.workflow.enums;


import lombok.Getter;

@Getter
public enum ValueGroupEnum {

    INPUTS("inputs"),
    OUTPUTS("outputs");

    private final String name;

    private ValueGroupEnum(String name){
        this.name=name;
    }

}
