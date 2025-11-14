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
}

