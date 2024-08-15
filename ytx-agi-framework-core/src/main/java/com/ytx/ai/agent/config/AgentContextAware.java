package com.ytx.ai.agent.config;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.agent.Intention.Intention;
import com.ytx.ai.agent.IntentionRegister;
import com.ytx.ai.agent.SkillRegister;
import com.ytx.ai.agent.skill.AiAgent;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

public class AgentContextAware implements ApplicationContextAware {
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, AiAgent> beansMap=applicationContext.getBeansOfType(AiAgent.class);
        if(ObjectUtil.isNotEmpty(beansMap)){
            beansMap.forEach((k,v)-> SkillRegister.register(v));
        }

        Map<String, Intention> intentionMap=applicationContext.getBeansOfType(Intention.class);
        if(ObjectUtil.isNotEmpty(intentionMap)){
            intentionMap.forEach((k,v)-> IntentionRegister.register(v));
        }
    }
}
