package com.ytx.ai.base.agent;

import cn.hutool.core.annotation.PropIgnore;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class AgentResponse {
    private Object result;
    private List<Command> commands;
    @JsonIgnore
    @PropIgnore
    private boolean needHumanFeedBack;
}
