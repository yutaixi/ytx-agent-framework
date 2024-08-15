package com.ytx.ai.agent.service;

import cn.hutool.json.JSONUtil;
import com.ytx.ai.agent.Intention.Intention;
import com.ytx.ai.agent.vo.PlannedTasks;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IntentionTaskService {

    public PlannedTasks findPlannedTasks(Intention intention){
        String tasksJson=intention.getSampleTasks();
        return JSONUtil.toBean(tasksJson,PlannedTasks.class);
    }
}
