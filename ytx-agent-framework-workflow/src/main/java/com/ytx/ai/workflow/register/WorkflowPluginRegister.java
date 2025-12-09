package com.ytx.ai.workflow.register;


import com.ytx.ai.workflow.node.WorkflowNode;

import java.util.HashMap;
import java.util.Map;

public class WorkflowPluginRegister {

    private static final Map<String, WorkflowNode> pluginMap = new HashMap<>();

    public static void register(WorkflowNode workflowNode) {
        if (workflowNode == null) {
            return;
        }
        pluginMap.put(workflowNode.getType(), workflowNode);
    }

    public static WorkflowNode get(String pluginType) {
        return pluginMap.get(pluginType);
    }
}
