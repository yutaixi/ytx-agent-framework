package com.ytx.ai.agent;

import com.ytx.ai.agent.Intention.Intention;
import com.ytx.ai.agent.skill.AiAgent;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class DefaultAgentHolder implements AgentHolder{

    @Autowired
    private IntentionRegister intentionRegister;
    @Autowired
    SkillRegister skillRegister;

    @Override
    public Intention getRefIntention(String intentName) {
        return intentionRegister.getIntention(intentName);
    }

    @Override
    public List<Intention> getRefIntentions() {
        return intentionRegister.getAllIntentions();
    }

    @Override
    public List<AiAgent> getRefSkills() {
        return skillRegister.getAllAgents();
    }

    @Override
    public String getProperty(String key) {
        return null;
    }
}
