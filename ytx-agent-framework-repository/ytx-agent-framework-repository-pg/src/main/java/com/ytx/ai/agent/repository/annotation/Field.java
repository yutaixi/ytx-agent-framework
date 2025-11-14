package com.ytx.ai.agent.repository.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * PostgreSQL 列注解
 * 用于定义字段在PostgreSQL中的列属性
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Field {

    /**
     * PostgreSQL 数据类型常量
     */
    interface Type {
        String VARCHAR = "VARCHAR";
        String TEXT = "TEXT";
        String INTEGER = "INTEGER";
        String BIGINT = "BIGINT";
        String REAL = "REAL";
        String DOUBLE_PRECISION = "DOUBLE PRECISION";
        String BOOLEAN = "BOOLEAN";
        String TIMESTAMP = "TIMESTAMP";
        String DATE = "DATE";
        String VECTOR = "VECTOR";
        String NUMERIC = "NUMERIC";
        String DECIMAL = "DECIMAL";
        String CHAR = "CHAR";
        String CHARACTER = "CHARACTER";
        String CHARACTER_VARYING = "CHARACTER VARYING";
        String SERIAL = "SERIAL";
        String BIGSERIAL = "BIGSERIAL";
        String JSONB = "JSONB";
    }

    /**
     * 列名（如果不指定，使用字段名）
     */
    String name() default "";

    /**
     * PostgreSQL 数据类型
     * 例如: VARCHAR, TEXT, INTEGER, BIGINT, TIMESTAMP, VECTOR等
     * 如果为空，则根据Java类型自动推断
     * 建议使用 Field.Type 常量，例如: Field.Type.VARCHAR
     */
    String type() default "";

    /**
     * 长度（用于VARCHAR等类型）
     */
    int length() default -1;

    /**
     * 精度（用于DECIMAL/NUMERIC类型）
     */
    int precision() default -1;

    /**
     * 标度（用于DECIMAL/NUMERIC类型）
     */
    int scale() default -1;

    /**
     * 向量维度（用于VECTOR类型）
     */
    int vectorDimension() default -1;

    /**
     * 是否允许为空
     */
    boolean nullable() default true;

    /**
     * 是否为主键
     */
    boolean primaryKey() default false;

    /**
     * 是否为唯一索引
     */
    boolean unique() default false;

    /**
     * 默认值（SQL表达式，例如: 'NOW()', 'DEFAULT'等）
     */
    String defaultValue() default "";

    /**
     * 是否在创建表时忽略此字段
     */
    boolean ignored() default false;
}

