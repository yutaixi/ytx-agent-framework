package com.ytx.ai.workflow;

import cn.hutool.core.annotation.Alias;
import cn.hutool.core.annotation.PropIgnore;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class WorkflowOutput {
    private Map<String, Value> outputs;
    private String answer;
    @PropIgnore
    @JsonIgnore
    private String costSummary;
    private boolean stopTheWorld;

    public static WorkflowOutput of() {
        WorkflowOutput output = new WorkflowOutput();
        output.setOutputs(new ConcurrentHashMap<>());
        return output;
    }

    public void addOutput(String name, Value value) {
        if (value == null) {
            return;
        }
        outputs.put(name, value);
    }
}
