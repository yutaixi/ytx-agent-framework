package com.ytx.ai.workflow;

import java.util.List;

public interface NodeMeta {

    default public List<Value> getInputs(){
        return null;
    }

    default public List<Value> getOutputs(){
        return null;
    }

    default public void setOutputs(List<Value> outputs){
    }
}
