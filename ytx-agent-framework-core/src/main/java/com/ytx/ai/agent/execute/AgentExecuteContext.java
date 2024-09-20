package com.ytx.ai.agent.execute;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.agent.vo.AgentMemory;
import com.ytx.ai.agent.vo.Command;
import com.ytx.ai.agent.vo.SubAgentResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class AgentExecuteContext {

    public AgentExecuteContext(){
        this.startTime=System.currentTimeMillis();
    }

    private boolean runAgent;
    private Long startTime;
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


    public boolean isRunAgent() {
        return runAgent;
    }

    public AgentExecuteContext setRunAgent(boolean runAgent) {
        this.runAgent = runAgent;
        return this;
    }
}
