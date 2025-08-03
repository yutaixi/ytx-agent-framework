package com.ytx.ai.workflow.service;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.base.enums.MemorySaveOption;
import com.ytx.ai.base.util.StringUtils;
import com.ytx.ai.base.agent.AgentMemory;
import com.ytx.ai.base.cache.CacheService;
import com.ytx.ai.base.web.RequestContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class AgentMemoryService {

    @Autowired
    private CacheService cacheService;

    private static final String CACHE_KEY_FORMAT="memory:%s:%s";
    private static final String DEFAULT_APP_ID="default_app_id";


    public AgentMemory getMemory(String chatId)
    {
        AgentMemory agentMemory=null;
        try{
            agentMemory=cacheService.getCacheObject(chatId);
        }catch (Exception e){
            log.error("getMemory error",e);
        }
        if(ObjectUtil.isNotNull(agentMemory) && ObjectUtil.isNotEmpty(agentMemory)){
            agentMemory.getMemoryMap().forEach((key,memory)->memory.setHasSaved(true));
        }
        return agentMemory;
    }

    public boolean saveMemory(String chatId,AgentMemory agentMemory){
        if(ObjectUtil.isNull(chatId) || ObjectUtil.isNull(agentMemory) || ObjectUtil.isEmpty(agentMemory.getMemoryMap())){
            return false;
        }
        AtomicInteger need2SaveCount=new AtomicInteger();
        agentMemory.getMemoryMap().forEach((key,memory)->{
            if(MemorySaveOption.IGNORE.equals(memory.getSaveOption()) || memory.isHasSaved()){
                return;
            }
            need2SaveCount.getAndIncrement();
        });

        if(need2SaveCount.get()<=0){
            return false;
        }
        try{
            cacheService.setCacheObject(formatCacheKey(chatId),agentMemory,3600, TimeUnit.SECONDS);
        }catch (Exception e){
            log.error("save memory error",e);
        }
        return true;
    }

    private String formatCacheKey(String chatId){
        String appId= RequestContextUtils.getRequest().getHeader("x-AppId");
        if(StringUtils.isBlank(appId))
        {
            appId=DEFAULT_APP_ID;
        }
        return String.format(CACHE_KEY_FORMAT,appId,chatId);
    }

}
