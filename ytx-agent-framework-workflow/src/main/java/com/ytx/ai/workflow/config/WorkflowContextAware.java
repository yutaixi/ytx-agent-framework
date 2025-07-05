package com.ytx.ai.workflow.config;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.workflow.plugin.Plugin;
import com.ytx.ai.workflow.register.WorkflowPluginRegister;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;
@Component
public class WorkflowContextAware implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Plugin> pluginMap = applicationContext.getBeansOfType(Plugin.class);
        if (ObjectUtil.isNotEmpty(pluginMap)) {
            pluginMap.forEach((k, v) -> {
                WorkflowPluginRegister.register(v);
            });
        }
    }
}
