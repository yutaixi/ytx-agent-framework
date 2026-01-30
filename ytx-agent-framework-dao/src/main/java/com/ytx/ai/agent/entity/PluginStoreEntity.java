package com.ytx.ai.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@TableName(value = "ai_plugin_store")
@Getter
@Setter
public class PluginStoreEntity {

    @TableId(type = IdType.AUTO) // 主键自动生成
    private Integer id;
    private Integer skillId;
    private String type;
    private String about;
    private String scenarios;
    private String category;
}
分析代码有哪些可以优化的点
