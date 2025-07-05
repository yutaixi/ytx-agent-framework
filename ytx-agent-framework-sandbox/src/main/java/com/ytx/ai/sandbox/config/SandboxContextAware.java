package com.ytx.ai.sandbox.config;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.sandbox.SandboxFunction;
import com.ytx.ai.sandbox.SandboxFunctionRegister;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class SandboxContextAware implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, SandboxFunction> sandboxFunctionMap = applicationContext.getBeansOfType(SandboxFunction.class);
        if (ObjectUtil.isNotEmpty(sandboxFunctionMap)) {
            sandboxFunctionMap.forEach((k, v) -> {
                SandboxFunctionRegister.register(v);
            });
        }
    }
}