package com.ytx.ai.workflow.enums;

public enum ValueTypeEnum {

    STRING("string"),
    INTEGER("integer"),
    BOOLEAN("boolean"),
    NUMBER("number"),
    TIME("time"),
    OBJECT("object"),
    ARRAY_STRING("array[string]"),
    ARRAY_INTEGER("array[integer]"),
    ARRAY_BOOLEAN("array[boolean]"),
    ARRAY_NUMBER("array[number]"),
    ARRAY_TIME("array[time]"),
    ARRAY_OBJECT("array[object]");

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
