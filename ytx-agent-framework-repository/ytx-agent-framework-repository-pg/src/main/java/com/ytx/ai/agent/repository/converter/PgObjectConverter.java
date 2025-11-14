package com.ytx.ai.agent.repository.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ytx.ai.agent.repository.exception.PgConversionException;
import com.ytx.ai.agent.repository.util.FieldTypeResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * PostgreSQL 对象转换器
 */
@Component
public class PgObjectConverter {

    private ObjectMapper objectMapper;
    private final PgVectorConverter vectorConverter;
    private final FieldTypeResolver fieldTypeResolver;

    @Autowired
    public PgObjectConverter(PgVectorConverter vectorConverter,
                             FieldTypeResolver fieldTypeResolver) {
        this.vectorConverter = vectorConverter;
        this.fieldTypeResolver = fieldTypeResolver;
    }

    @Autowired(required = false)
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
    }

    /**
     * 将 Bean 转换为 Map
     */
    public <T> Map<String, Object> beanToMap(T bean) {
        if (bean == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> raw = objectMapper.convertValue(bean, new TypeReference<Map<String, Object>>() {});

        // 清理 null 值并将复杂对象序列化为 JSON 字符串
        Map<String, Object> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            if (val == null) {
                continue;
            }

            // 处理向量字段（vector 类型）：根据值的类型自动识别，不依赖字段名
            if (vectorConverter.isVectorValue(val)) {
                cleaned.put(key, vectorConverter.convertToVectorPGobject(val));
            } else if (isSimpleValue(val)) {
                cleaned.put(key, val);
            } else {
                try {
                    cleaned.put(key, objectMapper.writeValueAsString(val));
                } catch (JsonProcessingException ex) {
                    // fallback to toString
                    cleaned.put(key, String.valueOf(val));
                }
            }
        }
        return cleaned;
    }

    /**
     * 将 Map 转换为 Bean 对象
     */
    @SuppressWarnings("unchecked")
    public <T> T mapToBean(Map<String, Object> map, Class<?> clazz) {
        if (map == null || clazz == null) {
            return null;
        }

        try {
            Map<String, Object> sanitized = new LinkedHashMap<>(map.size());
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                Object converted = convertValueForField(clazz, fieldName, value);
                sanitized.put(fieldName, converted);
            }
            return (T) objectMapper.convertValue(sanitized, clazz);
        } catch (Exception ex) {
            throw new PgConversionException("Failed to convert map to bean: " + ex.getMessage(), ex);
        }
    }

    /**
     * 为字段转换值
     */
    private Object convertValueForField(Class<?> clazz, String fieldName, Object value) {
        if (value == null || fieldName == null || clazz == null) {
            return value;
        }

        Class<?> fieldType = fieldTypeResolver.resolveFieldType(clazz, fieldName);
        if (fieldType == null) {
            return value;
        }

        // 处理向量类型
        if (fieldType.isArray() && fieldType.getComponentType() == float.class) {
            return vectorConverter.convertToPrimitiveFloatArray(value);
        }
        if (Float[].class.equals(fieldType)) {
            float[] primitive = vectorConverter.convertToPrimitiveFloatArray(value);
            Float[] boxed = new Float[primitive.length];
            for (int i = 0; i < primitive.length; i++) {
                boxed[i] = primitive[i];
            }
            return boxed;
        }

        // 处理 List<Float> 或 ArrayList<Float> 类型
        if (List.class.isAssignableFrom(fieldType) || Collection.class.isAssignableFrom(fieldType)) {
            Type genericType = fieldTypeResolver.resolveFieldGenericType(clazz, fieldName);
            if (genericType != null && genericType instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) genericType;
                Type[] actualTypes = paramType.getActualTypeArguments();
                if (actualTypes.length > 0 && actualTypes[0] == Float.class) {
                    // 字段类型是 List<Float> 或 ArrayList<Float>
                    // 如果值是 Map 类型（可能是 PGobject 被序列化后的结果），尝试提取值
                    if (value instanceof Map) {
                        // 尝试从 Map 中提取值，可能是 {"type":"vector","value":"[1.0,2.0,3.0]"} 格式
                        Map<?, ?> map = (Map<?, ?>) value;
                        if (map.containsKey("value")) {
                            value = map.get("value");
                        } else if (map.size() == 1 && map.values().iterator().hasNext()) {
                            // 如果只有一个值，尝试使用它
                            value = map.values().iterator().next();
                        } else {
                            // 如果 Map 包含多个键值对，尝试将其转换为数组
                            // 这种情况通常不应该发生，但为了健壮性，我们尝试处理
                            try {
                                value = objectMapper.writeValueAsString(value);
                            } catch (JsonProcessingException e) {
                                // 如果序列化失败，使用原始值
                            }
                        }
                    }
                    float[] primitive = vectorConverter.convertToPrimitiveFloatArray(value);
                    List<Float> floatList = new ArrayList<>(primitive.length);
                    for (float f : primitive) {
                        floatList.add(f);
                    }
                    return floatList;
                }
            }
        }

        return value;
    }

    /**
     * 判断是否为简单值类型
     */
    private boolean isSimpleValue(Object val) {
        return val instanceof CharSequence
                || val instanceof Number
                || val instanceof Boolean
                || val instanceof Date
                || val instanceof UUID;
    }

    /**
     * 从对象中获取字段值（用于排序）
     */
    public Object getFieldValue(Object obj, String fieldName) {
        if (obj == null || fieldName == null) {
            return null;
        }
        try {
            Map<String, Object> map = objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
            return map.get(fieldName);
        } catch (Exception ex) {
            return null;
        }
    }
}

