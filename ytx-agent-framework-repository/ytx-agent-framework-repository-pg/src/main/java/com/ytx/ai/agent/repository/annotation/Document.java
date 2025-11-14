package com.ytx.ai.agent.repository.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * PostgreSQL 表注解
 * 用于在类上定义表的相关属性
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Document {

    /**
     * 表名
     * 如果不指定，则使用类名（转换为下划线命名）
     */
    String indexName() default "";

    /**
     * 表所在的 schema
     * 默认为 "public"
     */
    String schema() default "public";
}
