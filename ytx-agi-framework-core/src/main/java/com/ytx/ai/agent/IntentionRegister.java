package com.ytx.ai.agent;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.agent.Intention.Intention;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IntentionRegister {

    private static final Map<String, Intention> intentions=new ConcurrentHashMap<>();

    private static final Map<String,Intention> intentionIdMap=new ConcurrentHashMap<>();

    public static void register(Intention intention){
        intentions.put(intention.getName(),intention);
        if(ObjectUtil.isNull(intention.getId()) || intention.getId()==-1){
            intentionIdMap.put(intention.getName(),intention);
        }else{
            intentionIdMap.put(String.valueOf(intention.getId()),intention);
        }
    }

    public Intention getIntention(String name){
        return intentions.get(name);
    }

    public Intention getIntention(int id){
        return intentionIdMap.get(String.valueOf(id));
    }

    public List<Intention> getAllIntentions(){
        return new ArrayList<>(intentions.values());
    }
}
