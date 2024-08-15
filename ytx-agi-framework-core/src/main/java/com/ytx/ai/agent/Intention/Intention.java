package com.ytx.ai.agent.Intention;

public interface Intention {

    default public Integer getId(){
        return -1;
    }
    public String getName();

    public String getDesc();

    public boolean intentRecognition();

    default public String getInstruction(){
        return "";
    }
    public String getSampleTasks();
}
