package com.ytx.ai.workflow.enums;

public enum PluginTypeIdEnum {

    START( "start",  "开始节点"),
    END( "end",  "结束节点"),
    LLM( "llm",  "调用大语言模型，使用变量和提示词生成回复"),
    HTTP( "http",  "http请求"),
    CONDITION( "condition",  "条件分支"),
    CODE( "code",  "编写代码，处理输入变量来生成返回值"),
    SUBPROCESS( "subProcess",  "子流程");

    private final String type;
    private final String description;

    private PluginTypeIdEnum(String type, String description) {
        this.type= type;
        this.description= description;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }
}
