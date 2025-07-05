package com.ytx.ai.workflow.execute;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.agent.dto.ChatDTO;
import com.ytx.ai.agent.vo.AgentMemory;
import com.ytx.ai.agent.vo.Command;
import com.ytx.ai.workflow.NodeOutput;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public class FlowContext {

    private ChatDTO chat;

    private Map<String, NodeOutput> nodeOutputMap;

    private WorkflowWrapper workflowWrapper;

    private AgentMemory agentMemory;

    private final List<Command> commands = new CopyOnWriteArrayList<>();

    public static FlowContext of() {
        FlowContext flowContext = new FlowContext();
        flowContext.setNodeOutputMap(new ConcurrentHashMap<>());
        return flowContext;
    }

    public void addNodeOutput(String nodeId, NodeOutput nodeOutput) {
        if (nodeOutputMap == null) {
            nodeOutputMap = new ConcurrentHashMap<>();
        }
        if (nodeOutput == null) {
            nodeOutput = NodeOutput.builder().build();
        }
        nodeOutputMap.put(nodeId, nodeOutput);
    }

    public FlowContext chat(ChatDTO chat) {
        this.chat = chat;
        return this;
    }


    public FlowContext workflowWrapper(WorkflowWrapper workflowWrapper) {
        this.workflowWrapper = workflowWrapper;
        return this;
    }

    public String getMemoryStr() {
        if (ObjectUtil.isNull(agentMemory)) {
            return "";
        }
        return agentMemory.toString();
    }

}
