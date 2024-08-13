package com.ytx.ai.agent.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class AgentResponse {
    String result;
    List<Command> commands;
}
