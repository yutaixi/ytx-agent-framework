package com.ytx.ai.agent.enums;

public enum MemorySaveOption {

    LONG_TERM("long_term","长期存储"),
    SHORT_TERM("short_term","短期存储"),
    IGNORE("ignore","忽略，不存储");

    private final String name;

    private final String description;

    private MemorySaveOption(String name,String description)
    {
        this.name=name;
        this.description=description;
    }
}
