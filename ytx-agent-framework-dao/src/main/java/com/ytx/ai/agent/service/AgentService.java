package com.ytx.ai.agent.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.ytx.ai.agent.IntentionRegister;
import com.ytx.ai.agent.entity.*;
import com.ytx.ai.agent.mapper.*;
import com.ytx.ai.agent.util.StringUtils;
import com.ytx.ai.agent.vo.AgentInfo;
import com.ytx.ai.base.cache.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ytx.ai.agent.constant.CacheConstants.LOCAL_CACHE_AGENT_INFO_KEY;
import static com.ytx.ai.agent.constant.CacheConstants.REDIS_AGENT_VER_KEY;

@Slf4j
public class AgentService {

    @Autowired
    IntentionMapper intentionMapper;

    @Autowired
    SkillMapper skillMapper;

    @Autowired
    AgentMapper agentMapper;
    @Autowired
    AgentRefIntentMapper agentRefIntentMapper;

    @Autowired
    AgentRefToolMapper agentRefToolMapper;

    @Autowired
    AgentPropertyMapper agentPropertyMapper;

    @Autowired
    private Cache<String,Object> caffeineCache;

    @Autowired
    CacheService cacheService;

    public boolean refreshSystemIntent()
    {
        List<IntentEntity> intents=intentionMapper.queryAll();
        if(ObjectUtil.isNotEmpty(intents)){
            intents.forEach(IntentionRegister::register);
        }
        return true;
    }

    public boolean refreshAgent(String agentCode){
        cacheService.setCacheObject(String.format(REDIS_AGENT_VER_KEY,agentCode),-1);
        return true;
    }

    public AgentInfo getAgentInfo(String agentCode){
        if(StringUtils.isBlank(agentCode)){
            return null;
        }
        Object cacheObj=caffeineCache.getIfPresent(String.format(LOCAL_CACHE_AGENT_INFO_KEY,agentCode));
        if(ObjectUtil.isNotNull(cacheObj) && cacheObj instanceof AgentInfo ){
            AgentInfo cacheVal=(AgentInfo) cacheObj;
            int latestVer=this.getAgentVerion(agentCode);
            if(cacheVal.getVersion()==latestVer || latestVer<-1){
                return cacheVal;
            }
        }

        AgentEntity agent=agentMapper.findAgent(agentCode);
        if(ObjectUtil.isNull(agent)){
            return null;
        }
        List<AgentRefIntentEntity> refIntents=agentRefIntentMapper.findAgentRefIntent(agent.getId());
        List<AgentRefSkillEntity> refSkills=agentRefToolMapper.findAgentRefSkills(agent.getId());
        List<AgentPropertyEntity> agentProperties=agentPropertyMapper.findAgentProperties(agent.getId());

        AgentInfo agentInfo=new AgentInfo();
        BeanUtils.copyProperties(agent,agentInfo);
        agentInfo.setId(agent.getId());
        agentInfo.setRefIntentions(refIntents);
        agentInfo.setRefSkills(refSkills);

        Map<String,String> properties=new HashMap<>();
        if(ObjectUtil.isNotEmpty(agentProperties)){
            agentProperties.forEach(agentPropertyEntity -> {
                properties.put(agentPropertyEntity.getPropertyName(),agentPropertyEntity.getPropertyValue());
            });
        }
        agentInfo.setAgentProperties(properties);
        updateCache(agentCode,agentInfo);
        return agentInfo;
    }

    private void updateCache(String agentCode,AgentInfo agentInfo){
        caffeineCache.put(String.format(LOCAL_CACHE_AGENT_INFO_KEY,agentCode),agentInfo);
        try{
            cacheService.setCacheObject(String.format(REDIS_AGENT_VER_KEY,agentCode),agentInfo );
        }catch (Exception e){
            log.error("update cache agent info error",e);
        }
    }

    public int getAgentVerion(String agentCode){
        Integer ver=null;
        try{
            Object cacheObj=cacheService.getCacheObject(String.format(REDIS_AGENT_VER_KEY,agentCode));
            if(ObjectUtil.isNull(cacheObj)){
                AgentEntity agentEntity=agentMapper.findAgent(agentCode);
                ver=agentEntity.getVersion();
            }else{
                AgentInfo agentInfo= JSONUtil.toBean(JSONUtil.toJsonStr(cacheObj),AgentInfo.class);
                ver=agentInfo.getVersion();
            }
        }catch (Exception e){
            ver=-100;
            log.error("get agent ver error",e);
        }
        return ver;
    }

}
