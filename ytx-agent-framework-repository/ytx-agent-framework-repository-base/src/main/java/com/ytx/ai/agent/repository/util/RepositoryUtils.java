package com.ytx.ai.agent.repository.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class RepositoryUtils {

    private static final org.slf4j.Logger log = getLogger();

    /**
     * 获取日志记录器，如果 SLF4J 不可用则返回 null
     */
    private static org.slf4j.Logger getLogger() {
        try {
            return org.slf4j.LoggerFactory.getLogger(RepositoryUtils.class);
        } catch (NoClassDefFoundError | Exception e) {
            return null;
        }
    }

    /**
     * 从对象或类上读取 Document 注解的 indexName 值
     * 支持以下两种 Document 注解：
     * 1. com.ytx.ai.agent.repository.annotation.Document
     * 2. org.springframework.data.elasticsearch.annotations.Document
     *
     * @param obj 目标对象或 Class 对象
     * @return indexName 值，如果未找到注解或发生异常则返回 null
     */
    public static String getIndexName(Object obj) {
        if (obj == null) {
            if (log != null) {
                log.error("getIndexName: obj parameter is null");
            }
            return null;
        }

        // 获取对象的 Class 对象
        Class<?> clazz;
        if (obj instanceof Class) {
            clazz = (Class<?>) obj;
        } else {
            clazz = obj.getClass();
        }

        // 定义支持的 Document 注解类名
        String[] documentAnnotationNames = {
            "com.ytx.ai.agent.repository.annotation.Document",
            "org.springframework.data.elasticsearch.annotations.Document"
        };

        for (String annotationName : documentAnnotationNames) {
            try {
                // 使用反射获取 Document 注解类
                Class<?> documentClass = Class.forName(annotationName);
                Annotation document = clazz.getAnnotation(documentClass.asSubclass(Annotation.class));

                if (document != null) {
                    // 使用反射调用 indexName() 方法
                    Method indexNameMethod = documentClass.getMethod("indexName");
                    String result = (String) indexNameMethod.invoke(document);
                    return result;
                }
            } catch (ClassNotFoundException e) {
                // 注解类不存在，继续尝试下一个
                if (log != null) {
                    log.debug("Document annotation class not found: {}", annotationName);
                }
            } catch (NoSuchMethodException e) {
                // indexName 方法不存在
                if (log != null) {
                    log.error("indexName method not found in annotation: {}", annotationName, e);
                }
            } catch (Exception e) {
                // 其他异常
                if (log != null) {
                    log.error("Error getting indexName from annotation: {} for class: {}",
                        annotationName, clazz.getName(), e);
                }
            }
        }

        return null;
    }

}
