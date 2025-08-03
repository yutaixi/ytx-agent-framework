package com.ytx.ai.agent.service;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
//import com.ytx.ai.agent.IntentionRegister;
import com.ytx.ai.agent.entity.*;
import com.ytx.ai.agent.mapper.*;
import com.ytx.ai.base.util.StringUtils;
import com.ytx.ai.agent.vo.PageSearchVO;
import com.ytx.ai.agent.vo.PageVO;
import com.ytx.ai.base.cache.CacheService;
import com.ytx.ai.base.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import static com.ytx.ai.agent.constant.CacheConstants.*;

@Slf4j
public class AgentService extends ServiceImpl<AgentMapper,AgentEntity> {

    @Autowired
    IntentionMapper intentionMapper;

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


    public PageVO<AgentEntity> queryAgentPage(PageSearchVO<AgentEntity> pageSearchVO){
        // 创建分页对象
        Page<AgentEntity> pageRequest = new Page<>(pageSearchVO.getCurrent(), pageSearchVO.getSize());

        AgentEntity agent=pageSearchVO.getData();
        // 构造查询条件
        QueryWrapper<AgentEntity> queryWrapper = new QueryWrapper<>();
        if(agent!=null &&ObjectUtil.isNotEmpty(agent.getName())){
            queryWrapper.like("name",agent.getName());
        }
        Page<AgentEntity> pagedRecords= getBaseMapper().selectPage(pageRequest, queryWrapper);
        return PageVO.of(pagedRecords);
    }

    public Integer upsertAgent(AgentEntity agent){
        Integer agentId=agent.getId();
        if(ObjectUtil.isNotEmpty(agentId)){
            this.getBaseMapper().updateById(agent);
            deleteCache(agent.getId());
        }else{
            agent.setCode(IdGenerator.next());
            agent.setType("workflow");
            this.getBaseMapper().insert(agent);
            agentId=agent.getId();
        }
        return agentId;
    }

    public boolean deleteAgent(Integer agentId){
        if(ObjectUtil.isEmpty(agentId)){
            return false;
        }
        deleteCache(agentId);
        return this.getBaseMapper().deleteById(agentId)==1;
    }

    public AgentEntity findAgent(Integer agentId){
        if (ObjectUtil.isEmpty(agentId)) {
            return null;
        }
        Object cachedObj = caffeineCache.getIfPresent(String.format(LOCAL_CACHE_AGENT_DETAIL_KEY, agentId));
        if (ObjectUtil.isNotNull(cachedObj) && cachedObj instanceof AgentEntity cachedVal) {
            int latestVer = this.getAgentVersion(agentId);
            if ((cachedVal.getVer() == latestVer) || latestVer < -1) {
                return cachedVal;
            }
        }

        AgentEntity agent=getBaseMapper().selectById(agentId);
        if(ObjectUtil.isNull(agent)){
            return null;
        }

        updateCache(agentId,agent);
        return agent;
    }

//    public boolean refreshSystemIntent()
//    {
//        List<IntentEntity> intents=intentionMapper.queryAll();
//        if(ObjectUtil.isNotEmpty(intents)){
//            intents.forEach(IntentionRegister::register);
//        }
//        return true;
//    }

    public boolean refreshAgent(String agentCode){
        cacheService.setCacheObject(String.format(REDIS_AGENT_VER_KEY,agentCode),-1);
        return true;
    }

    public AgentEntity getAgentInfo(String agentCode){
        if(StringUtils.isBlank(agentCode)){
            return null;
        }
        Object cacheObj=caffeineCache.getIfPresent(String.format(LOCAL_CACHE_AGENT_INFO_KEY,agentCode));
        if(ObjectUtil.isNotNull(cacheObj) && cacheObj instanceof AgentEntity cacheVal){
            int latestVer=this.getAgentVersion(agentCode);
            if(cacheVal.getVer()==latestVer || latestVer<-1){
                return cacheVal;
            }
        }

        AgentEntity agent=getBaseMapper().findAgent(agentCode);
        if(ObjectUtil.isNull(agent)){
            return null;
        }

        updateCache(agentCode,agent);
        return agent;
    }

    private void updateCache(String agentCode,AgentEntity agentEntity){
        caffeineCache.put(String.format(LOCAL_CACHE_AGENT_INFO_KEY,agentCode),agentEntity);
        try{
            if(ObjectUtil.isNotEmpty(agentEntity.getVer())){
                cacheService.setCacheString(String.format(REDIS_AGENT_VER_KEY,agentCode),String.valueOf(agentEntity.getVer()) );
            }
        }catch (Exception e){
            log.error("update cache agent info error",e);
        }
    }

    private void updateCache(Integer agentId,AgentEntity agentEntity){
        caffeineCache.put(String.format(LOCAL_CACHE_AGENT_DETAIL_KEY,agentId),agentEntity);
        try{
            if(ObjectUtil.isNotEmpty(agentEntity.getVer())){
                cacheService.setCacheString(String.format(REDIS_AGENT_VER_KEY,agentId),String.valueOf(agentEntity.getVer()) );
            }
        }catch (Exception e){
            log.error("update cache agent info error",e);
        }
    }

    public int getAgentVersion(String agentCode){
        Integer ver=null;
        try{
            String cachedVer=cacheService.getCacheString(String.format(REDIS_AGENT_VER_KEY,agentCode));
            if(ObjectUtil.isEmpty(cachedVer)){
                AgentEntity agentEntity=getBaseMapper().findAgent(agentCode);
                ver=agentEntity.getVer();
            }else{
                ver=Integer.valueOf(cachedVer);
            }
        }catch (Exception e){
            ver=-100;
            log.error("get agent ver error",e);
        }
        return ver;
    }

    public int getAgentVersion(Integer agentId){
        Integer ver=null;
        try{
            String cachedVer=cacheService.getCacheObject(String.format(REDIS_AGENT_VER_KEY,agentId));
            if(ObjectUtil.isNull(cachedVer)){
                AgentEntity agentEntity=getBaseMapper().selectById(agentId);
                ver=agentEntity.getVer();
                cacheService.setCacheString(String.format(REDIS_AGENT_VER_KEY,agentId),String.valueOf(ver));
            }else{
                ver=Integer.valueOf(cachedVer);
            }
        }catch (Exception e){
            ver=-100;
            log.error("get agent ver error",e);
        }
        return ver;
    }

    private void deleteCache(Integer agentId) {
        caffeineCache.invalidate(String.format(LOCAL_CACHE_AGENT_DETAIL_KEY, agentId));
        caffeineCache.invalidate(String.format(LOCAL_CACHE_AGENT_INFO_KEY, agentId));
        try {
            cacheService.deleteKey(String.format(REDIS_AGENT_VER_KEY, agentId));
        } catch (Exception e) {
            log.error("delete cache agent info error", e);
        }
    }

}
