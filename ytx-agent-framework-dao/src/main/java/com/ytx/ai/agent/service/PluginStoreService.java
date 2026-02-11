package com.ytx.ai.agent.service;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ytx.ai.agent.entity.PluginStoreEntity;
import com.ytx.ai.agent.entity.SkillEntity;
import com.ytx.ai.agent.entity.SkillToolEntity;
import com.ytx.ai.agent.mapper.PluginStoreMapper;
import com.ytx.ai.agent.vo.PageSearchVO;
import com.ytx.ai.agent.vo.PageVO;
import com.ytx.ai.agent.vo.PluginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 插件商店服务类
 * 提供插件商店的增删改查功能
 */
@Slf4j
public class PluginStoreService extends ServiceImpl<PluginStoreMapper, PluginStoreEntity> {

    @Autowired
    private SkillService skillService;

    @Autowired
    private SkillToolService skillToolService;

    /**
     * 分页查询插件商店列表
     *
     * @param pageSearchVO 分页查询条件对象，包含查询条件实体和分页参数
     * @return 分页结果对象，包含当前页数据和总记录数
     */
    public PageVO<PluginVO> queryPluginStorePage(PageSearchVO<PluginStoreEntity> pageSearchVO) {
        // 1. 兜底处理分页参数，避免空指针
        PageSearchVO<PluginStoreEntity> safeSearchVO = ObjectUtil.isNull(pageSearchVO) ? new PageSearchVO<>() : pageSearchVO;
        Page<PluginStoreEntity> pageRequest = new Page<>(safeSearchVO.getCurrent(), safeSearchVO.getSize());

        // 2. 构造查询条件（根据传入参数动态组装）
        PluginStoreEntity queryEntity = safeSearchVO.getData();
        QueryWrapper<PluginStoreEntity> queryWrapper = buildPluginStoreQueryWrapper(queryEntity);

        // 3. 执行分页查询
        Page<PluginStoreEntity> pagedRecords = this.getBaseMapper().selectPage(pageRequest, queryWrapper);

        // 4. 批量查询 Skill，避免逐条查询导致的 N+1 问题
        Map<Integer, SkillEntity> skillEntityMap = loadSkillEntityMap(pagedRecords.getRecords());

        // 5. 转换 Entity 为 VO 并填充 Skill 信息
        List<PluginVO> pluginVOList = pagedRecords.getRecords().stream().map(storeEntity -> {
            PluginVO vo = new PluginVO();
            // 5.1 填充 PluginStore 信息
            vo.setId(storeEntity.getId());
            vo.setSkillId(storeEntity.getSkillId());
            vo.setType(storeEntity.getType());
            vo.setAbout(storeEntity.getAbout());
            vo.setScenarios(storeEntity.getScenarios());
            vo.setCategory(storeEntity.getCategory());
            vo.setOfficial(storeEntity.getOfficial());
            // 5.2 填充 Skill 信息
            if (ObjectUtil.isNotNull(storeEntity.getSkillId())) {
                SkillEntity skill = skillEntityMap.get(storeEntity.getSkillId());
                if (skill != null) {
                    vo.setName(skill.getName());
                    vo.setDescription(skill.getDescription());
                    vo.setVer(skill.getVer());
                    vo.setIcon(skill.getIcon());
                }
            }
            return vo;
        }).collect(Collectors.toList());

        // 6. 构造新的 Page 对象用于返回 (因为类型变了)
        Page<PluginVO> resultPage = new Page<>(pagedRecords.getCurrent(), pagedRecords.getSize(), pagedRecords.getTotal());
        resultPage.setRecords(pluginVOList);

        // 7. 将查询结果封装为统一的分页VO对象返回
        return PageVO.of(resultPage);
    }

    /**
     * 构造插件商店查询条件
     *
     * @param queryEntity 插件商店查询条件实体
     * @return MyBatis-Plus 查询条件包装器
     */
    private QueryWrapper<PluginStoreEntity> buildPluginStoreQueryWrapper(PluginStoreEntity queryEntity) {
        QueryWrapper<PluginStoreEntity> queryWrapper = new QueryWrapper<>();
        if (ObjectUtil.isNull(queryEntity)) {
            return queryWrapper;
        }

        // 如果传入了类型，则进行精确匹配查询
        if (ObjectUtil.isNotEmpty(queryEntity.getType())) {
            queryWrapper.eq("type", queryEntity.getType());
        }
        // 如果传入了分类，则进行精确匹配查询
        if (ObjectUtil.isNotEmpty(queryEntity.getCategory())) {
            queryWrapper.eq("category", queryEntity.getCategory());
        }
        // 如果传入了about信息，则进行模糊查询
        if (ObjectUtil.isNotEmpty(queryEntity.getAbout())) {
            queryWrapper.like("about", queryEntity.getAbout());
        }
        // 如果传入了scenarios信息，则进行模糊查询
        if (ObjectUtil.isNotEmpty(queryEntity.getScenarios())) {
            queryWrapper.like("scenarios", queryEntity.getScenarios());
        }
        return queryWrapper;
    }

    /**
     * 批量加载 Skill 信息并构建映射
     *
     * @param storeEntityList 插件商店实体列表
     * @return SkillId -> SkillEntity 的映射
     */
    private Map<Integer, SkillEntity> loadSkillEntityMap(List<PluginStoreEntity> storeEntityList) {
        if (ObjectUtil.isEmpty(storeEntityList)) {
            return Collections.emptyMap();
        }

        // 提取 SkillId 列表并去重，减少数据库查询数量
        Set<Integer> skillIdSet = storeEntityList.stream()
                .map(PluginStoreEntity::getSkillId)
                .filter(ObjectUtil::isNotNull)
                .collect(Collectors.toSet());
        if (ObjectUtil.isEmpty(skillIdSet)) {
            return Collections.emptyMap();
        }

        List<SkillEntity> skillEntityList = skillService.listByIds(skillIdSet);
        if (ObjectUtil.isEmpty(skillEntityList)) {
            return Collections.emptyMap();
        }

        return skillEntityList.stream()
                .filter(ObjectUtil::isNotNull)
                .collect(Collectors.toMap(SkillEntity::getId, skill -> skill, (left, right) -> left));
    }

    /**
     * 通过ID查询插件商店详情
     *
     * @param id 插件商店ID
     * @return 插件商店实体对象，如果未找到则返回null
     */
    public PluginVO getPluginById(Integer id) {
        // 1. 校验ID是否为空
        if (ObjectUtil.isEmpty(id)) {
            return null;
        }
        // 2. 调用BaseMapper的selectById方法根据主键查询
        PluginStoreEntity storeEntity = this.getBaseMapper().selectById(id);
        if (storeEntity == null) {
            return null;
        }

        // 2. 转换为 PluginVO 并填充基本信息
        PluginVO vo = new PluginVO();
        vo.setId(storeEntity.getId());
        vo.setSkillId(storeEntity.getSkillId());
        vo.setType(storeEntity.getType());
        vo.setAbout(storeEntity.getAbout());
        vo.setScenarios(storeEntity.getScenarios());
        vo.setCategory(storeEntity.getCategory());
        vo.setOfficial(storeEntity.getOfficial());
        // 3. 填充 Skill 信息和 SkillTool 信息
        if (ObjectUtil.isNotNull(storeEntity.getSkillId())) {
            // 3.1 填充 Skill 信息
            SkillEntity skill = skillService.findSkill(storeEntity.getSkillId());
            if (skill != null) {
                vo.setName(skill.getName());
                vo.setDescription(skill.getDescription());
                vo.setVer(skill.getVer());
                vo.setIcon(skill.getIcon());
            }

            // 3.2 填充 SkillTool 信息
            List<SkillToolEntity> tools = skillToolService.querySkillToolListBySkillId(storeEntity.getSkillId());
            vo.setTools(tools);
        }

        return vo;
    }

    /**
     * 新增插件商店信息
     *
     * @param pluginStoreEntity 待新增的插件商店实体对象
     * @return 新增记录的ID
     */
    public Integer addPluginStore(PluginStoreEntity pluginStoreEntity) {
        // 1. 调用BaseMapper的insert方法插入数据
        this.getBaseMapper().insert(pluginStoreEntity);
        // 2. 返回生成的ID
        return pluginStoreEntity.getId();
    }

    /**
     * 通过ID更新插件商店信息
     *
     * @param pluginStoreEntity 待更新的插件商店实体对象，必须包含ID
     * @return 更新后的记录ID，如果ID为空则返回null
     */
    public Integer updatePluginStore(PluginStoreEntity pluginStoreEntity) {
        // 1. 校验ID是否存在
        if (ObjectUtil.isEmpty(pluginStoreEntity.getId())) {
            return null;
        }
        // 2. 调用BaseMapper的updateById方法根据ID更新非空字段
        this.getBaseMapper().updateById(pluginStoreEntity);
        return pluginStoreEntity.getId();
    }

    /**
     * 通过ID删除插件商店信息
     *
     * @param id 待删除的插件商店ID
     * @return 删除是否成功
     */
    public boolean deletePluginStore(Integer id) {
        // 1. 校验ID是否为空
        if (ObjectUtil.isEmpty(id)) {
            return false;
        }
        // 2. 调用BaseMapper的deleteById方法删除数据
        int rows = this.getBaseMapper().deleteById(id);
        // 3. 返回是否删除成功（影响行数大于0）
        return rows > 0;
    }
}

