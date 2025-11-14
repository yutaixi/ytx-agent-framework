package com.ytx.ai.agent.repository.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * PostgreSQL Repository 配置类
 */
@Component
@ConfigurationProperties(prefix = "pg.repository")
public class PgRepositoryConfig {

    /**
     * 主键字段名
     */
    private String primaryKeyField = "id";

    /**
     * 业务唯一标识字段名
     */
    private String businessIdField = "bid";

    /**
     * 是否允许创建索引（表）
     * 为防止意外创建表，默认为false
     */
    private boolean enableCreateIndex = false;

    /**
     * 是否允许删除索引（表）
     * 为防止意外删除表，默认为false
     */
    private boolean enableDeleteIndex = false;

    public String getPrimaryKeyField() {
        return primaryKeyField;
    }

    public void setPrimaryKeyField(String primaryKeyField) {
        this.primaryKeyField = primaryKeyField;
    }

    public String getBusinessIdField() {
        return businessIdField;
    }

    public void setBusinessIdField(String businessIdField) {
        this.businessIdField = businessIdField;
    }

    public boolean isEnableCreateIndex() {
        return enableCreateIndex;
    }

    public void setEnableCreateIndex(boolean enableCreateIndex) {
        this.enableCreateIndex = enableCreateIndex;
    }

    public boolean isEnableDeleteIndex() {
        return enableDeleteIndex;
    }

    public void setEnableDeleteIndex(boolean enableDeleteIndex) {
        this.enableDeleteIndex = enableDeleteIndex;
    }
}

