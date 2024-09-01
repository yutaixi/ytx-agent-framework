package com.ytx.ai.agent.service;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.agent.AgentHolder;
import com.ytx.ai.agent.Intention.Intention;
import com.ytx.ai.agent.IntentionRegister;
import com.ytx.ai.agent.SkillRegister;
import com.ytx.ai.agent.entity.AgentRefIntentEntity;
import com.ytx.ai.agent.entity.AgentRefSkillEntity;
import com.ytx.ai.agent.interceptor.RequestContext;
import com.ytx.ai.agent.skill.AiAgent;
import com.ytx.ai.agent.vo.AgentInfo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class DBBasedAgentHolder implements AgentHolder  {

    @Autowired
    AgentService agentService;

    @Autowired
    IntentionRegister intentionRegister;

    @Autowired
    SkillRegister skillRegister;

    @Override
    public Intention getRefIntention(String intentName) {
        return intentionRegister.getIntention(intentName);
    }

    @Override
    public List<Intention> getRefIntentions() {
        AgentInfo agentInfo=agentService.getAgentInfo(RequestContext.getAgentCode());
        if(ObjectUtil.isNull(agentInfo)){
            return null;
        }
        List<AgentRefIntentEntity> refIntentions=agentInfo.getRefIntentions();
        if(ObjectUtil.isEmpty(refIntentions)){
            return null;
        }
        List<Intention> intentions=new ArrayList<>();
        refIntentions.forEach(intent->{
            Intention registerIntent=intentionRegister.getIntention(intent.getIntentId());
            if(ObjectUtil.isNotNull(registerIntent) && registerIntent.intentRecognition()){
                intentions.add(registerIntent);
            }
        });

        return intentions;
    }

    @Override
    public List<AiAgent> getRefSkills() {
        AgentInfo agentInfo=agentService.getAgentInfo(RequestContext.getAgentCode());
        if(ObjectUtil.isNull(agentInfo)){
            return null;
        }
        List<AgentRefSkillEntity> refSkills=agentInfo.getRefSkills();
        if(ObjectUtil.isEmpty(refSkills)){
            return null;
        }
        List<AiAgent> skills=new ArrayList<>();
        refSkills.forEach(skill->{
            AiAgent aiAgent=skillRegister.getAgent(skill.getName());
            if(ObjectUtil.isNotNull(aiAgent) && aiAgent.isParticipateLlmPlanning()) {
                skills.add(aiAgent);
            }
        });

        return skills;
    }

    @Override
    public String getProperty(String key) {
        AgentInfo agentInfo=agentService.getAgentInfo(RequestContext.getAgentCode());
        if(ObjectUtil.isNull(agentInfo) || ObjectUtil.isEmpty(agentInfo.getAgentProperties())){
            return null;
        }
        return agentInfo.getAgentProperties().get(key);
    }
}
