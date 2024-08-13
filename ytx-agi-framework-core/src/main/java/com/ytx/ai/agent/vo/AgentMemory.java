package com.ytx.ai.agent.vo;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.agent.util.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class AgentMemory implements Serializable {


    Map<String,MemoryTrace> memoryMap=new ConcurrentHashMap<>();

    public String toString()
    {
        if(CollectionUtil.isEmpty(memoryMap))
        {
            return "None.";
        }
        StringBuilder builder=new StringBuilder();
        memoryMap.forEach((key,value)->builder.append("- ").append(key).append(":").append(value).append("\n"));
        return builder.toString();
    }

    public boolean put(String key,String value){
        if(StringUtils.isBlank(key)){
            return false;
        }
        return this.put(MemoryTrace.builder().name(key).value(value).build());
    }

    public boolean putAll(List<MemoryTrace> memorys){
        if(ObjectUtil.isEmpty(memorys)){
            return false;
        }
        memorys.forEach(this::put);
        return true;
    }

    public boolean put(MemoryTrace memoryTrace)
    {
        MemoryTrace oldMemory=memoryMap.get(memoryTrace.getName());
        if(!memoryTrace.isUserAssigned() && ObjectUtil.isNotNull(oldMemory) && oldMemory.isUserAssigned())
        {
            return false;
        }
        memoryMap.put(memoryTrace.getName(),memoryTrace);
        return true;
    }
}
