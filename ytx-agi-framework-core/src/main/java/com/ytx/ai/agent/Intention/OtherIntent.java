package com.ytx.ai.agent.Intention;

public class OtherIntent implements Intention{

    public static final String NAME="other";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDesc() {
        return "other intention";
    }

    @Override
    public boolean intentRecognition() {
        return true;
    }

    @Override
    public String getSampleTasks() {
        return null;
    }
}
