package com.ytx.ai.agent.repository.converter;

import com.ytx.ai.agent.repository.exception.PgConversionException;
import com.ytx.ai.agent.repository.util.SimilarityCalculator;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * PostgreSQL 向量转换器
 */
@Component
public class PgVectorConverter {

    private final SimilarityCalculator similarityCalculator;

    @Autowired
    public PgVectorConverter(SimilarityCalculator similarityCalculator) {
        this.similarityCalculator = similarityCalculator;
    }

    /**
     * 检查值是否为向量类型
     */
    public boolean isVectorValue(Object val) {
        return val instanceof List<?>
                || val instanceof float[]
                || val instanceof Float[]
                || val instanceof double[]
                || val instanceof Double[];
    }

    /**
     * 将向量值转换为 PostgreSQL vector 类型的 PGobject
     */
    public PGobject convertToVectorPGobject(Object val) {
        try {
            float[] floatArray = convertToPrimitiveFloatArray(val);
            // pgvector 格式: [1.0,2.0,3.0]
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < floatArray.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(floatArray[i]);
            }
            sb.append("]");

            PGobject pgObject = new PGobject();
            pgObject.setType("vector");
            pgObject.setValue(sb.toString());
            return pgObject;
        } catch (SQLException ex) {
            throw new PgConversionException("Failed to convert embedding to vector: " + ex.getMessage(), ex);
        }
    }

    /**
     * 将向量值转换为 PostgreSQL vector 格式的字符串
     * 例如：[0.02, 0.03, 0.04, 0.05] -> "0.02,0.03,0.04,0.05"
     */
    public String convertVectorValueToString(Object value) {
        if (value == null) {
            throw new PgConversionException("SIMILAR search: vector value cannot be null.");
        }
        float[] vector = convertToPrimitiveFloatArray(value);
        if (vector.length == 0) {
            throw new PgConversionException("SIMILAR search: vector value cannot be empty.");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector[i]);
        }
        return sb.toString();
    }

    /**
     * 将距离转换为相似度分数
     */
    public float distanceToSimilarity(float distance) {
        return similarityCalculator.calculate(distance);
    }

    /**
     * 将各种类型的值转换为原始 float 数组
     */
    public float[] convertToPrimitiveFloatArray(Object value) {
        if (value == null) {
            return new float[0];
        }
        if (value instanceof float[]) {
            return (float[]) value;
        }
        if (value instanceof Float[] floats) {
            float[] result = new float[floats.length];
            for (int i = 0; i < floats.length; i++) {
                result[i] = floats[i] == null ? 0f : floats[i];
            }
            return result;
        }
        if (value instanceof double[] doubles) {
            float[] result = new float[doubles.length];
            for (int i = 0; i < doubles.length; i++) {
                result[i] = (float) doubles[i];
            }
            return result;
        }
        if (value instanceof Double[] doubles) {
            float[] result = new float[doubles.length];
            for (int i = 0; i < doubles.length; i++) {
                result[i] = doubles[i] == null ? 0f : doubles[i].floatValue();
            }
            return result;
        }
        if (value instanceof Collection<?> collection) {
            float[] result = new float[collection.size()];
            int idx = 0;
            for (Object item : collection) {
                result[idx++] = parseNumberToFloat(item);
            }
            return result;
        }
        if (value instanceof Object[] array) {
            float[] result = new float[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = parseNumberToFloat(array[i]);
            }
            return result;
        }
        if (value instanceof Array sqlArray) {
            try {
                Object array = sqlArray.getArray();
                return convertToPrimitiveFloatArray(array);
            } catch (SQLException e) {
                throw new PgConversionException("Failed to read SQL array: " + e.getMessage(), e);
            }
        }
        if (value instanceof PGobject pgObject) {
            String type = pgObject.getType();
            if ("vector".equalsIgnoreCase(type) || "json".equalsIgnoreCase(type) || "jsonb".equalsIgnoreCase(type)) {
                return convertToPrimitiveFloatArray(pgObject.getValue());
            }
        }
        if (value instanceof String str) {
            return parseFloatVectorString(str);
        }
        return new float[]{parseNumberToFloat(value)};
    }

    private float parseNumberToFloat(Object value) {
        if (value == null) {
            return 0f;
        }
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return Float.parseFloat(String.valueOf(value));
    }

    private float[] parseFloatVectorString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[0];
        }
        String cleaned = text.trim();
        if ((cleaned.startsWith("[") && cleaned.endsWith("]")) || (cleaned.startsWith("{") && cleaned.endsWith("}"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        if (cleaned.isEmpty()) {
            return new float[0];
        }
        String[] parts = cleaned.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parseNumberToFloat(parts[i].trim());
        }
        return result;
    }
}

