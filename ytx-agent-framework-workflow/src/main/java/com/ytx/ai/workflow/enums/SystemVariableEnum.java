package com.ytx.ai.workflow.enums;

public enum SystemVariableEnum {

    USER_INPUT("userInput", "用户输入的内容"),
    CHAT_HISTORY("chatHistory", "聊天历史"),
    MEMORY("memory", "记忆");

    private final String name;
    private final String description;

    private SystemVariableEnum(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static boolean isSystemVariable(String name) {
        for (SystemVariableEnum v : SystemVariableEnum.values()) {
            if (v.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public static SystemVariableEnum of(String name) {
        for (SystemVariableEnum v : SystemVariableEnum.values()) {
            if (v.getName().equalsIgnoreCase(name)) {
                return v;
            }
        }
        return null;
    }

}
