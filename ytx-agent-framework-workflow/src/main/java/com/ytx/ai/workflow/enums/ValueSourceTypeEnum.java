package com.ytx.ai.workflow.enums;

public enum ValueSourceTypeEnum {
    LITERAL("literal"),
    REFERENCE("ref");

    private final String source;

    private ValueSourceTypeEnum(String type) {
        this.source = type;
    }

    public String getSource() {
        return source;
    }

    public static boolean isLiteral(String source) {
        return LITERAL.getSource().equalsIgnoreCase(source);
    }

    public static boolean isReference(String source) {
        return REFERENCE.getSource().equalsIgnoreCase(source);
    }
}
