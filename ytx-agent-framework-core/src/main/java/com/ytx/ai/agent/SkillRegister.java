package com.ytx.ai.agent;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.agent.skill.AiAgent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

public class SkillRegister {

    private static final Map<String, AiAgent> agentMap=new ConcurrentHashMap<>();

    public static void register(AiAgent agent){
        agentMap.put(agent.getName(),agent);
    }

    public AiAgent getAgent(String name){
        return agentMap.get(name);
    }

    public List<AiAgent> getAllAgents(){
        return new ArrayList<>(agentMap.values());
    }

    public String getAllAgentNames(){
        if(ObjectUtil.isEmpty(agentMap)){
            return "";
        }
        StringJoiner joiner=new StringJoiner(",");
        agentMap.values().forEach(aiAgent->joiner.add(aiAgent.getName()));
        return joiner.toString();
    }
}
