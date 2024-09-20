package com.ytx.ai.agent.entity;

import com.ytx.ai.agent.Intention.Intention;
import lombok.Getter;
import lombok.Setter;

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
