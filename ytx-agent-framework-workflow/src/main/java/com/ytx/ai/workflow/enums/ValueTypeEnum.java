package com.ytx.ai.workflow.enums;

public enum ValueTypeEnum {

    STRING("string"),
    INTEGER("integer"),
    BOOLEAN("boolean"),
    NUMBER("number"),
    TIME("time"),
    OBJECT("object");

    private final String type;

    private ValueTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static ValueTypeEnum of(String type) {
        ValueTypeEnum result = null;
        for (ValueTypeEnum item : ValueTypeEnum.values()) {
            if (item.getType().equalsIgnoreCase(type)) {
                result = item;
                break;
            }
        }
        return result;
    }
}
