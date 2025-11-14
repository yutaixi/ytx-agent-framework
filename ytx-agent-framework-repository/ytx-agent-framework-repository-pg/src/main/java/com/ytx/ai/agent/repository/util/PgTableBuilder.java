package com.ytx.ai.agent.repository.util;

import com.ytx.ai.agent.repository.annotation.Field;
import com.ytx.ai.agent.repository.annotation.Document;
import com.ytx.ai.agent.repository.config.PgRepositoryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL 表构建器
 * 根据类定义和注解生成 CREATE TABLE DDL 语句
 */
@Component
public class PgTableBuilder {

    private final PgRepositoryConfig config;
    private final PgIdentifierEscaper escaper;

    @Autowired
    public PgTableBuilder(PgRepositoryConfig config, PgIdentifierEscaper escaper) {
        this.config = config;
        this.escaper = escaper;
    }

    /**
     * 根据类定义生成 CREATE TABLE DDL 语句
     */
    public String buildCreateTableSql(String tableName, Class<?> clazz) {
        List<ColumnDefinition> columns = parseColumns(clazz);

        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Class " + clazz.getName() + " has no valid columns to create table");
        }

        // 读取 @PgTable 注解，获取 schema
        String schema = "public";
        Document tableAnnotation = clazz.getAnnotation(Document.class);
        if (tableAnnotation != null && !tableAnnotation.schema().isEmpty()) {
            schema = tableAnnotation.schema();
        }

        StringBuilder sql = new StringBuilder();
        String fullTableName;
        if (schema != null && !schema.isEmpty() && !"public".equals(schema)) {
            fullTableName = escaper.escape(schema) + "." + escaper.escape(tableName);
        } else {
            fullTableName = escaper.escape(tableName);
        }
        sql.append("CREATE TABLE IF NOT EXISTS ").append(fullTableName).append(" (\n");

        List<String> columnDefs = new ArrayList<>();
        List<String> constraints = new ArrayList<>();

        for (ColumnDefinition col : columns) {
            columnDefs.add("    " + buildColumnDefinition(col));

            if (col.isPrimaryKey) {
                constraints.add("PRIMARY KEY (" + escaper.escape(col.name) + ")");
            }
            if (col.isUnique && !col.isPrimaryKey) {
                constraints.add("UNIQUE (" + escaper.escape(col.name) + ")");
            }
        }

        // 添加所有列定义
        sql.append(String.join(",\n", columnDefs));

        // 添加约束
        if (!constraints.isEmpty()) {
            sql.append(",\n");
            sql.append("    ").append(String.join(",\n    ", constraints));
        }

        sql.append("\n)");

        return sql.toString();
    }

    /**
     * 解析类的所有字段，生成列定义列表
     */
    private List<ColumnDefinition> parseColumns(Class<?> clazz) {
        List<ColumnDefinition> columns = new ArrayList<>();
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            java.lang.reflect.Field[] fields = current.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                // 跳过静态字段和合成字段
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                    || field.isSynthetic()) {
                    continue;
                }

                Field annotation = field.getAnnotation(Field.class);

                // 如果注解标记为忽略，跳过
                if (annotation != null && annotation.ignored()) {
                    continue;
                }

                ColumnDefinition col = buildColumnDefinition(field, annotation);
                if (col != null) {
                    columns.add(col);
                }
            }
            current = current.getSuperclass();
        }

        return columns;
    }

    /**
     * 构建列定义对象
     */
    private ColumnDefinition buildColumnDefinition(java.lang.reflect.Field field, Field annotation) {
        ColumnDefinition col = new ColumnDefinition();

        // 确定列名
        if (annotation != null && !annotation.name().isEmpty()) {
            col.name = annotation.name();
        } else {
            col.name = field.getName();
        }

        // 确定列类型
        Class<?> fieldType = field.getType();
        if (annotation != null && !annotation.type().isEmpty()) {
            col.type = annotation.type();
        } else {
            col.type = inferPostgresType(fieldType, annotation);
        }

        // 确定长度、精度等属性
        if (annotation != null) {
            col.length = annotation.length();
            col.precision = annotation.precision();
            col.scale = annotation.scale();
            col.vectorDimension = annotation.vectorDimension();
            col.nullable = annotation.nullable();
            col.isPrimaryKey = annotation.primaryKey();
            col.isUnique = annotation.unique();
            col.defaultValue = annotation.defaultValue();
        } else {
            // 默认值：根据字段名推断
            col.isPrimaryKey = config.getPrimaryKeyField().equalsIgnoreCase(col.name);
            col.isUnique = config.getBusinessIdField().equalsIgnoreCase(col.name);
            col.nullable = true;
        }

        // 构建完整的列类型字符串
        col.type = buildTypeString(col.type, col.length, col.precision, col.scale, col.vectorDimension);

        return col;
    }

    /**
     * 根据Java类型推断PostgreSQL类型
     */
    private String inferPostgresType(Class<?> fieldType, Field annotation) {
        // 如果是向量类型
        if (annotation != null && annotation.vectorDimension() > 0) {
            return Field.Type.VECTOR;
        }

        // 处理数组类型（可能是向量）
        if (fieldType.isArray()) {
            Class<?> componentType = fieldType.getComponentType();
            if (componentType == float.class || componentType == Float.class) {
                return Field.Type.VECTOR;
            }
        }

        // 处理List类型（可能是向量）
        if (java.util.List.class.isAssignableFrom(fieldType)
            || java.util.Collection.class.isAssignableFrom(fieldType)) {
            // 默认假设可能是向量，如果注解指定了维度则确定为向量
            if (annotation != null && annotation.vectorDimension() > 0) {
                return Field.Type.VECTOR;
            }
        }

        // 基本类型映射
        if (fieldType == String.class) {
            return Field.Type.TEXT;
        } else if (fieldType == Integer.class || fieldType == int.class) {
            return Field.Type.INTEGER;
        } else if (fieldType == Long.class || fieldType == long.class) {
            return Field.Type.BIGINT;
        } else if (fieldType == Float.class || fieldType == float.class) {
            return Field.Type.REAL;
        } else if (fieldType == Double.class || fieldType == double.class) {
            return Field.Type.DOUBLE_PRECISION;
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            return Field.Type.BOOLEAN;
        } else if (java.util.Date.class.isAssignableFrom(fieldType)
                   || java.sql.Timestamp.class.isAssignableFrom(fieldType)) {
            return Field.Type.TIMESTAMP;
        } else if (java.time.LocalDateTime.class.isAssignableFrom(fieldType)) {
            return Field.Type.TIMESTAMP;
        } else if (java.time.LocalDate.class.isAssignableFrom(fieldType)) {
            return Field.Type.DATE;
        } else {
            // 默认使用TEXT或JSONB
            return Field.Type.TEXT;
        }
    }

    /**
     * 构建类型字符串（包含长度、精度等）
     */
    private String buildTypeString(String baseType, int length, int precision, int scale, int vectorDimension) {
        String type = baseType.toUpperCase();

        if (Field.Type.VARCHAR.equals(type) || Field.Type.CHARACTER_VARYING.equals(type)) {
            if (length > 0) {
                return type + "(" + length + ")";
            }
            return Field.Type.VARCHAR + "(255)"; // 默认长度
        } else if (Field.Type.CHAR.equals(type) || Field.Type.CHARACTER.equals(type)) {
            if (length > 0) {
                return type + "(" + length + ")";
            }
            return Field.Type.CHAR + "(1)";
        } else if (Field.Type.NUMERIC.equals(type) || Field.Type.DECIMAL.equals(type)) {
            if (precision > 0) {
                if (scale > 0) {
                    return type + "(" + precision + "," + scale + ")";
                }
                return type + "(" + precision + ")";
            }
            return type;
        } else if (Field.Type.VECTOR.equals(type)) {
            if (vectorDimension > 0) {
                return "vector(" + vectorDimension + ")";
            }
            return "vector"; // 如果没有指定维度，需要在后面检查
        }

        return type;
    }

    /**
     * 构建列定义SQL片段
     */
    private String buildColumnDefinition(ColumnDefinition col) {
        StringBuilder sb = new StringBuilder();
        sb.append(escaper.escape(col.name));

        // 如果主键是BIGINT或INTEGER类型，使用BIGSERIAL或SERIAL
        String actualType = col.type;
        if (col.isPrimaryKey) {
            // 提取基础类型（去除参数部分）
            String baseType = col.type.split("\\(")[0].toUpperCase();
            if (Field.Type.BIGINT.equals(baseType)) {
                actualType = Field.Type.BIGSERIAL;
            } else if (Field.Type.INTEGER.equals(baseType)) {
                actualType = Field.Type.SERIAL;
            }
        }

        sb.append(" ").append(actualType);

        if (!col.nullable) {
            sb.append(" NOT NULL");
        }

        if (!col.defaultValue.isEmpty()) {
            sb.append(" DEFAULT ").append(col.defaultValue);
        }

        return sb.toString();
    }

    /**
     * 从类中获取表名（从 @PgTable 注解或类名推断）
     */
    public String getTableName(Class<?> clazz) {
        Document tableAnnotation = clazz.getAnnotation(Document.class);
        if (tableAnnotation != null && !tableAnnotation.indexName().isEmpty()) {
            return tableAnnotation.indexName();
        }
        // 如果没有注解或注解中没有指定表名，从类名推断（驼峰转下划线）
        return camelToSnake(clazz.getSimpleName());
    }

    /**
     * 从类中获取 schema（从 @PgTable 注解）
     */
    public String getSchema(Class<?> clazz) {
        Document tableAnnotation = clazz.getAnnotation(Document.class);
        if (tableAnnotation != null && !tableAnnotation.schema().isEmpty()) {
            return tableAnnotation.schema();
        }
        return "public";
    }

    /**
     * 将驼峰命名转换为下划线命名
     */
    private String camelToSnake(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 列定义内部类
     */
    private static class ColumnDefinition {
        String name;
        String type;
        int length = -1;
        int precision = -1;
        int scale = -1;
        int vectorDimension = -1;
        boolean nullable = true;
        boolean isPrimaryKey = false;
        boolean isUnique = false;
        String defaultValue = "";
    }
}

