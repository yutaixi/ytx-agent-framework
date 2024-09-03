package com.ytx.ai.agent.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentTask {
    String task_id;
    String agent;
    String objective;
    String context;
    Integer execute_order;
    Object execute_result;
    Object params;
}
