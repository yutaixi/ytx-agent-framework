package com.ytx.ai.agent.vo;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PlannedTasks {

    List<AgentTask> tasks;

    public String gatTaskName()
    {
        if(ObjectUtil.isEmpty(tasks)){
            return "";
        }
        StringBuilder builder=new StringBuilder();
        tasks.forEach(task->builder.append(task.getAgent()).append(","));
        return builder.toString();
    }
}
