package com.ytx.ai.agent;

import com.ytx.ai.agent.Intention.Intention;
import com.ytx.ai.agent.skill.AiAgent;

import java.util.List;

public interface AgentHolder {

    public Intention getRefIntention(String intentName);

    public List<Intention> getRefIntentions();

    public List<AiAgent> getRefSkills();

    public String getProperty(String key);

}
