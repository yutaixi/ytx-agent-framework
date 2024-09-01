package com.ytx.ai.agent.entity;

import com.ytx.ai.agent.Intention.Intention;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IntentEntity implements Intention {

    String name;
    String desc;
    String instruction;
    String sampleTasks;
    boolean intentRecognition;

    @Override
    public boolean intentRecognition() {
        return intentRecognition;
    }
}
