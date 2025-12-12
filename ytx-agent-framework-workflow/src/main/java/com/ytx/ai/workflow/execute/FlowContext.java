package com.ytx.ai.workflow.execute;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.base.agent.AgentMemory;
import com.ytx.ai.base.agent.ChatDTO;
import com.ytx.ai.base.agent.Command;
import com.ytx.ai.base.agent.Skill;
import com.ytx.ai.workflow.NodeResult;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public class FlowContext {

    private boolean strictMode = true;
    private ChatDTO chat;
    private Map<String, NodeResult> nodeOutputMap;
    private WorkflowWrapper workflowWrapper;
    private AgentMemory agentMemory;
    private Map<String, Skill> skillMap =new HashMap<>();

    private final List<Command> commands = new CopyOnWriteArrayList<>();


    public static FlowContext of(FlowContext context) {
        FlowContext flowContext = new FlowContext();
        flowContext.setChat(context.getChat());
        flowContext.setAgentMemory(context.getAgentMemory());
        flowContext.setSkillMap(context.getSkillMap());
        Map<String,NodeResult> outputMap=context.getNodeOutputMap();
        if(ObjectUtil.isNotEmpty(outputMap)){
            Map<String,NodeResult> newMap=new ConcurrentHashMap<>(outputMap);
            flowContext.setNodeOutputMap(newMap);
        }
        flowContext.setStrictMode(context.isStrictMode());
        return flowContext;
    }

    public static FlowContext of() {
        FlowContext flowContext = new FlowContext();
        flowContext.setNodeOutputMap(new ConcurrentHashMap<>());
        return flowContext;
    }
    public static FlowContext of(ChatDTO chatDTO) {
        FlowContext flowContext = new FlowContext();
        flowContext.setNodeOutputMap(new ConcurrentHashMap<>());
        flowContext.setChat(chatDTO);
        return flowContext;
    }

    public void addNodeOutput(String nodeId, NodeResult nodeResult) {
        if (nodeOutputMap == null) {
            nodeOutputMap = new ConcurrentHashMap<>();
        }
        if (nodeResult == null) {
            nodeResult = NodeResult.builder().build();
        }
        nodeOutputMap.put(nodeId, nodeResult);
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
