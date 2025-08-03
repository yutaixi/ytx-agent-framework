package com.ytx.ai.workflow.execute;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.base.agent.AgentMemory;
import com.ytx.ai.base.agent.Command;
import com.ytx.ai.base.agent.Skill;
import com.ytx.ai.base.agent.SubAgentResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class AgentExecuteContext {

    public AgentExecuteContext(){
        this.startTime=System.currentTimeMillis();
    }

    public Map<String, Skill> skillMap;
    private final Long startTime;
    private AgentMemory agentMemory;
    final List<Command> commands=new CopyOnWriteArrayList<>();
    final Map<String, SubAgentResponse> context=new ConcurrentHashMap<>();

    public boolean put(String key,SubAgentResponse value){
        context.put(key,value);
        return true;
    }

    public <T> T getBean(String key,Class<T> clazz){
        if(ObjectUtil.isNull(context.get(key))){
            return null;
        }
        return context.get(key).getBean(clazz);
    }

    public SubAgentResponse get(String key){
        return context.get(key);
    }

    public AgentMemory getMemory(){
        return this.agentMemory;
    }
    public String getMemoryStr(){
        if(ObjectUtil.isNull(agentMemory)){
            return "";
        }
        return agentMemory.toString();
    }

    public void setMemory(AgentMemory memory){
        this.agentMemory=memory;
    }

    public List<Command> getCommands(){
        return commands;
    }
    public Long getStartTime(){
        return startTime;
    }

    public AgentExecuteContext setSkillMap(Map<String, Skill> skillMap) {
        this.skillMap = skillMap;
        return this;
    }

    public Map<String, Skill> getSkillMap() {
        return skillMap;
    }

    public Skill getSkill(String id){
        return skillMap.get(id);
    }

}
