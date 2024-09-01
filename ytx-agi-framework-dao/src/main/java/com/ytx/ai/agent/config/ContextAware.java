package com.ytx.ai.agent.config;


import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.agent.IntentionRegister;
import com.ytx.ai.agent.entity.IntentEntity;
import com.ytx.ai.agent.mapper.IntentionMapper;
import com.ytx.ai.agent.mapper.SkillMapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

@Configuration
@Component
public class ContextAware implements ApplicationContextAware {

    @Autowired
    IntentionMapper intentionMapper;
    @Autowired
    SkillMapper skillMapper;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        List<IntentEntity> intents=intentionMapper.queryAll();

        if(ObjectUtil.isNotEmpty(intents)){
            intents.forEach(IntentionRegister::register);
        }
    }
}
