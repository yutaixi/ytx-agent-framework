package com.ytx.ai.workflow;

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

    /**
     * 每个节点的执行跟踪信息，key 为节点 ID，value 为该节点的详细运行信息。
     * 使用 ConcurrentHashMap 保证多线程写入安全（各节点在不同工作线程中执行完成后写入）。
     */
    private Map<String, NodeTraceInfo> nodeTraces;

    public static WorkflowOutput of() {
        WorkflowOutput output = new WorkflowOutput();
        output.setOutputs(new ConcurrentHashMap<>());
        output.setNodeTraces(new ConcurrentHashMap<>());
        return output;
    }

    public void addOutput(String name, Value value) {
        if (value == null) {
            return;
        }
        outputs.put(name, value);
    }

    /**
     * 添加节点执行跟踪信息。
     *
     * @param nodeId    节点 ID
     * @param traceInfo 节点执行跟踪信息
     */
    public void addNodeTrace(String nodeId, NodeTraceInfo traceInfo) {
        if (nodeId == null || traceInfo == null) {
            return;
        }
        nodeTraces.put(nodeId, traceInfo);
    }
}