package com.ytx.ai.workflow.register;


import com.ytx.ai.workflow.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class WorkflowPluginRegister {

    private static final Map<String, Plugin> pluginMap = new HashMap<>();

    public static void register(Plugin plugin) {
        if (plugin == null) {
            return;
        }
        pluginMap.put(plugin.getType(), plugin);
    }

    public static Plugin get(String pluginType) {
        return pluginMap.get(pluginType);
    }
}
