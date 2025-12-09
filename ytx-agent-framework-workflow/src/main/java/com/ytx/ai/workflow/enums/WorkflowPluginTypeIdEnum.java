package com.ytx.ai.workflow.enums;

public enum WorkflowPluginTypeIdEnum {

    START( "start",  "开始节点"),
    END( "end",  "结束节点"),
    LLM( "llm",  "调用大语言模型，使用变量和提示词生成回复"),
    HTTP( "http",  "http请求"),
    CONDITION( "condition",  "条件分支"),
    CODE( "code",  "编写代码，处理输入变量来生成返回值"),
    SUBPROCESS( "subProcess",  "子流程"),
    BATCH( "batch",  "批量处理"),
    BATCH_BODY( "batchBody",  "批量处理体"),


    AGENT_START("agentStart",  "启动智能体"),
    AGENT_END("agentEnd",  "结束智能体"),
    AGENT_INTENT("agentIntent",  "智能体意图"),
    AGENT_PLAN("agentPlan",  "智能体计划"),
    AGENT_EXECUTOR("agentExecutor",  "智能体执行器"),
    AGENT_REPLY("agentReply",  "智能体回复");


    private final String type;
    private final String description;

    private WorkflowPluginTypeIdEnum(String type, String description) {
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
