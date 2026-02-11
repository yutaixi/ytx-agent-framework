package com.ytx.ai.agent.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ytx.ai.agent.entity.SkillEntity;
import com.ytx.ai.agent.entity.SkillToolEntity;
import com.ytx.ai.agent.mapper.SkillToolMapper;
import com.ytx.ai.base.agent.HeaderDefinition;
import com.ytx.ai.base.agent.PluginDefinition;
import com.ytx.ai.base.agent.ToolDefinition;
import com.ytx.ai.base.agent.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能工具服务类
 * 提供技能工具的增删改查功能
 */
@Slf4j
public class SkillToolService extends ServiceImpl<SkillToolMapper, SkillToolEntity> {

    @Autowired
    private SkillService skillService;
    /**
     * 创建技能工具
     *
     * @param skillToolEntity 技能工具实体，包含技能工具的详细信息
     * @return boolean 创建成功返回 true，否则返回 false
     */
    public boolean createSkillTool(SkillToolEntity skillToolEntity) {
        // 直接调用 MyBatis Plus 提供的 save 方法保存实体
        return this.save(skillToolEntity);
    }

    /**
     * 更新技能工具
     *
     * @param skillToolEntity 技能工具实体，必须包含 ID
     * @return boolean 更新成功返回 true，否则返回 false
     */
    public boolean updateSkillTool(SkillToolEntity skillToolEntity) {
        // 直接调用 MyBatis Plus 提供的 updateById 方法根据 ID 更新实体
        return this.updateById(skillToolEntity);
    }

    /**
     * 通过 skillId 查询返回集合
     *
     * @param skillId 技能 ID，用于筛选关联的工具
     * @return List<SkillToolEntity> 返回匹配的技能工具列表
     */
    public List<SkillToolEntity> querySkillToolListBySkillId(Integer skillId) {
        // 构建查询条件
        QueryWrapper<SkillToolEntity> queryWrapper = new QueryWrapper<>();
        // 根据 skill_id 字段进行相等查询，对应数据库中的 skill_id 列
        queryWrapper.eq("skill_id", skillId);
        // 执行查询并返回列表
        return this.list(queryWrapper);
    }

    /**
     * 通过 id 查询返回单个记录
     *
     * @param id 技能工具 ID
     * @return SkillToolEntity 返回对应的技能工具实体，如果不存在则返回 null
     */
    public SkillToolEntity getSkillToolById(Integer id) {
        // 直接调用 MyBatis Plus 提供的 getById 方法查询
        return this.getById(id);
    }

    /**
     * 更新状态
     *
     * @param id     技能工具 ID
     * @param disabled 状态（true: 禁用，false: 启用）
     * @return boolean 更新成功返回 true，否则返回 false
     */
    public boolean updateDisabled(Integer id, Boolean disabled) {
        if (id == null || disabled == null) {
            return false;
        }
        UpdateWrapper<SkillToolEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("disabled", disabled);
        updateWrapper.eq("id", id);
        return this.update(updateWrapper);
    }

    /**
     * 根据 id 删除功能的接口
     *
     * @param id 技能工具 ID
     * @return boolean 删除成功返回 true，否则返回 false
     */
    public boolean deleteSkillToolById(Integer id) {
        // 直接调用 MyBatis Plus 提供的 removeById 方法删除
        return this.removeById(id);
    }

    public Object run(Integer toolId,Map<String,Object> params){
        SkillToolEntity skillToolEntity= getSkillToolById(toolId);
        return run(skillToolEntity,params);
    }

    /**
     * 执行技能工具
     * 根据工具定义的入参和出参，完成工具的调用
     * 1. 拼接请求地址：插件URL + 工具Path
     * 2. 设置请求头：插件定义的Headers
     * 3. 处理入参：根据参数定义的位置(method)填充参数
     * 4. 发送请求并返回结果
     *
     * @param skillToolEntity 技能工具实体
     * @param params          运行时参数
     * @return Object 调用结果
     */
    public Object run(SkillToolEntity skillToolEntity, Map<String, Object> params) {
        SkillEntity skill = skillService.findSkill(skillToolEntity.getSkillId());
        if (skill == null) {
            throw new RuntimeException("Skill not found: " + skillToolEntity.getSkillId());
        }
        PluginDefinition skillDefinition = JSONUtil.toBean(skill.getDefinition(), PluginDefinition.class);
        ToolDefinition toolDefinition = JSONUtil.toBean(skillToolEntity.getDefinition(), ToolDefinition.class);

        // 1. 构建完整请求 URL
        String url = skillDefinition.getUrl() + toolDefinition.getPath();

        // 2. 处理 Path 参数
        List<ToolParam> inputs = toolDefinition.getInputs();
        Map<String, Object> bodyParams = new HashMap<>();

        if (inputs != null) {
            for (ToolParam input : inputs) {
                String paramName = input.getName();
                Object paramValue = params.get(paramName);
                if (paramValue == null) continue;

                String method = input.getMethod();
                // 默认处理逻辑
                if (StrUtil.isBlank(method)) {
                    if ("Get".equalsIgnoreCase(toolDefinition.getMethod())) {
                        method = "Query";
                    } else {
                        method = "Body";
                    }
                }

                if ("Path".equalsIgnoreCase(method)) {
                    url = url.replace("{" + paramName + "}", paramValue.toString());
                }
            }
        }

        HttpRequest request = HttpUtil.createRequest(Method.valueOf(toolDefinition.getMethod().toUpperCase()), url);

        // 3. 设置 Header
        List<HeaderDefinition> headers = skillDefinition.getHeaders();
        if (headers != null) {
            for (HeaderDefinition header : headers) {
                request.header(header.getKey(), header.getValue());
            }
        }

        // 4. 设置参数 (Query, Header, Body)
        if (inputs != null) {
            for (ToolParam input : inputs) {
                String paramName = input.getName();
                Object paramValue = params.get(paramName);
                if (paramValue == null) continue;

                String method = input.getMethod();
                if (StrUtil.isBlank(method)) {
                    if ("Get".equalsIgnoreCase(toolDefinition.getMethod())) {
                        method = "Query";
                    } else {
                        method = "Body";
                    }
                }

                if ("Query".equalsIgnoreCase(method)) {
                    request.form(paramName, paramValue);
                } else if ("Header".equalsIgnoreCase(method)) {
                    request.header(paramName, paramValue.toString());
                } else if ("Body".equalsIgnoreCase(method)) {
                    bodyParams.put(paramName, paramValue);
                }
            }
        }

        if (!bodyParams.isEmpty()) {
            request.body(JSONUtil.toJsonStr(bodyParams));
        }

        // 5. 执行请求
        String responseBody = request.execute().body();

        // 6. 处理输出
        if (JSONUtil.isTypeJSON(responseBody)) {
            return JSONUtil.parse(responseBody);
        }
        return responseBody;
    }
    public Object debug(SkillToolEntity skillToolEntity,Map<String,Object> params){
        return run(skillToolEntity,params);
    }
}

