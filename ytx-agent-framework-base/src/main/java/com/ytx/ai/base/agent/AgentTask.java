package com.ytx.ai.base.agent;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AgentTask {
    private String task_id;
    private String objective;
    private String agent_id;
    private String function;
    private Integer execute_order;
    private Map<String,Object> params;
    private Object execute_result;
}
