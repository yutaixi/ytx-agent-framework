package com.ytx.ai.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ytx.ai.base.agent.Intention;
import lombok.Getter;
import lombok.Setter;
@TableName("ai_intention")
@Getter
@Setter
public class IntentEntity implements Intention {

    private String name;
    private String desc;
    private String instruction;
    private String sampleTasks;
    private boolean intentRecognition;

    @Override
    public boolean intentRecognition() {
        return intentRecognition;
    }
}
