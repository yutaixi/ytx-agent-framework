package com.ytx.ai.agent.vo;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class SubAgentResponse {
    String agentName;
    boolean needHumanFeedback;
    Object result;
    List<Command> commands;
    List<MemoryTrace> memoryTraces;

    public <T> T getBean(Class<T> clazz){
        if(ObjectUtil.isNull(result) || needHumanFeedback){
            return null;
        }
        return JSONUtil.toBean(JSONUtil.toJsonStr(result),clazz);
    }
}
