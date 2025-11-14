package com.ytx.ai.agent.repository.util;

import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 * 字段类型解析器
 */
@Component
public class FieldTypeResolver {

    /**
     * 解析字段类型
     */
    public Class<?> resolveFieldType(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName).getType();
            } catch (NoSuchFieldException ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    /**
     * 解析字段的泛型类型
     */
    public Type resolveFieldGenericType(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName).getGenericType();
            } catch (NoSuchFieldException ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    /**
     * 判断是否为向量类型
     */
    public boolean isVectorType(Class<?> fieldType) {
        return (fieldType.isArray() && fieldType.getComponentType() == float.class)
                || Float[].class.equals(fieldType)
                || List.class.isAssignableFrom(fieldType)
                || Collection.class.isAssignableFrom(fieldType);
    }
}

