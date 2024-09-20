package com.ytx.ai.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@TableName("ai_agent")
@Getter
@Setter
public class AgentEntity {

    private Integer id;
    private String name;
    private String description;
    private int version;
}
