package com.ytx.ai.agent.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentTask {
    private String task_id;
    private String agent;
    private String objective;
    private String context;
    private Integer execute_order;
    private Object execute_result;
    private Object params;
}
