package com.ytx.ai.agent.service;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.ytx.ai.agent.entity.SkillEntity;
import com.ytx.ai.agent.mapper.SkillMapper;
import com.ytx.ai.agent.vo.PageSearchVO;
import com.ytx.ai.agent.vo.PageVO;
import com.ytx.ai.base.cache.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;


import static com.ytx.ai.agent.constant.CacheConstants.LOCAL_CACHE_SKILL_INFO_KEY;
import static com.ytx.ai.agent.constant.CacheConstants.REDIS_SKILL_VER_KEY;

@Slf4j
public class SkillService extends ServiceImpl<SkillMapper,SkillEntity> {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private Cache<String, Object> caffeineCache;

    public SkillEntity findSkill(Integer skillId) {
        if (ObjectUtil.isEmpty(skillId)) {
            return null;
        }

        Object cachedObj = caffeineCache.getIfPresent(String.format(LOCAL_CACHE_SKILL_INFO_KEY, skillId));
        if (ObjectUtil.isNotNull(cachedObj) && cachedObj instanceof SkillEntity cachedVal) {
            int latestVer = this.getSkillVersion(skillId);
            if ((cachedVal.getVer()!=null && cachedVal.getVer() == latestVer) || latestVer < -1) {
                return cachedVal;
            }
        }

        SkillEntity skillEntity = this.getBaseMapper().find(skillId);
        if (ObjectUtil.isNull(skillEntity)) {
            return null;
        }

        updateCache(skillId, skillEntity);
        return skillEntity;
    }

    public boolean upsertSkill(SkillEntity skill){
        if(ObjectUtil.isNotEmpty(skill.getId())){
            this.getBaseMapper().updateById(skill);
            deleteCache(skill.getId());
        }else{
            this.getBaseMapper().insert(skill);
        }
        return true;
    }

    public PageVO<SkillEntity> querySkill(PageSearchVO<SkillEntity> pageSearchVO) {
        // 创建分页对象
        Page<SkillEntity> pageRequest = new Page<>(pageSearchVO.getCurrent(), pageSearchVO.getSize());

        SkillEntity skill=pageSearchVO.getData();
        // 构造查询条件
        QueryWrapper<SkillEntity> queryWrapper = new QueryWrapper<>();

        // 根据传入的实体动态构造查询条件（示例）
        if (ObjectUtil.isNotEmpty(skill.getName())) {
            queryWrapper.like("name", skill.getName()); // 按名称模糊查询
        }
        if (ObjectUtil.isNotEmpty(skill.getType())) {
            queryWrapper.eq("type", skill.getType()); // 按类型精确查询
        }
        // 执行分页查询
        Page<SkillEntity> pagedRecords= this.getBaseMapper().selectPage(pageRequest, queryWrapper);
        return PageVO.of(pagedRecords);
    }

    public boolean deleteSkill(Integer skillId){
        this.getBaseMapper().deleteById(skillId);
        deleteCache(skillId);
        return true;
    }

    private void updateCache(Integer skillId, SkillEntity skillEntity) {
        caffeineCache.put(String.format(LOCAL_CACHE_SKILL_INFO_KEY, skillId), skillEntity);
        String ver=ObjectUtil.isNotEmpty(skillEntity.getVer())?skillEntity.getVer().toString():null;
        if(ObjectUtil.isEmpty(ver)){
            return;
        }
        try {
            cacheService.setCacheObject(String.format(REDIS_SKILL_VER_KEY, skillId),ver);
        } catch (Exception e) {
            log.error("update cache skill info error", e);
        }
    }

    private void deleteCache(Integer skillId) {
        caffeineCache.invalidate(String.format(LOCAL_CACHE_SKILL_INFO_KEY, skillId));
        try {
            cacheService.deleteKey(String.format(REDIS_SKILL_VER_KEY, skillId));
        } catch (Exception e) {
            log.error("delete cache skill info error", e);
        }
    }

    public int getSkillVersion(Integer skillId) {
        Integer ver=null;
        try {
            String verStr = cacheService.getCacheString(String.format(REDIS_SKILL_VER_KEY, skillId));
            if (ObjectUtil.isNull(verStr)) {
                ver = -1;
            }
            ver=Integer.valueOf(verStr);
        } catch (Exception e) {
            ver = -100;
            log.error("get agent ver error", e);
        }
        return ver;
    }
}
