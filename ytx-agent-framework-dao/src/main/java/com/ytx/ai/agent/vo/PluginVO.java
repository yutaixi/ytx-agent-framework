package com.ytx.ai.agent.vo;

import com.ytx.ai.agent.entity.SkillToolEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 插件信息VO
 * 整合PluginStore和Skill的信息
 */
@Getter
@Setter
public class PluginVO {

    /**
     * 插件商店ID
     */
    private Integer id;

    /**
     * 插件名称 (来自Skill)
     */
    private String name;

    /**
     * 插件图标
     */
    private String icon;

    /**
     * 插件描述 (来自Skill)
     */
    private String description;

    /**
     * 插件类型
     */
    private String type;

    /**
     * 插件版本 (来自Skill)
     */
    private Integer ver;

    /**
     * 技能ID
     */
    private Integer skillId;

    /**
     * 插件关于信息
     */
    private String about;

    /**
     * 插件适用场景
     */
    private String scenarios;

    /**
     * 插件分类
     */
    private String category;


    private List<SkillToolEntity> tools;


    private Boolean official;
}
