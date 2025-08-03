package com.ytx.ai.workflow.register;


import com.ytx.ai.workflow.plugin.WorkflowPlugin;

import java.util.HashMap;
import java.util.Map;

public class WorkflowPluginRegister {

    private static final Map<String, WorkflowPlugin> pluginMap = new HashMap<>();

    public static void register(WorkflowPlugin workflowPlugin) {
        if (workflowPlugin == null) {
            return;
        }
        pluginMap.put(workflowPlugin.getType(), workflowPlugin);
    }

    public static WorkflowPlugin get(String pluginType) {
        return pluginMap.get(pluginType);
    }
}
