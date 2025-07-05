package com.ytx.ai.workflow.util;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.annotation.DependsRef;
import com.ytx.ai.workflow.annotation.DependsVariable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NodeReflectUtils {


    /**
     * 根据引用的名字，组别，获取引用目标的Value值
     * @param nodeMeta
     * @param vName
     * @param vGroup
     * @return
     */
    public static Value getValue(NodeMeta nodeMeta,String vName,String vGroup){

        Value targetValue=null;
        List<Field> fields = getAllFields(nodeMeta.getClass());
        if(ObjectUtil.isEmpty(fields)){
            return null;
        }
        if(ObjectUtil.isEmpty(vGroup)){
            Optional<Field> field= fields.stream().filter(item->{
                return item.getName().equalsIgnoreCase(vName);
            }).findFirst();

            if(field.isPresent()){
                try {
                    field.get().setAccessible(true);
                    Object valueObj=field.get().get(nodeMeta);
                    if(valueObj instanceof Value value){
                        targetValue=value;
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }else{
            Optional<Field> field= fields.stream().filter(item->{
                return item.getName().equalsIgnoreCase(vGroup);
            }).findFirst();
            if(field.isPresent()){
                try {
                    field.get().setAccessible(true);
                    Object valueObj=field.get().get(nodeMeta);
                    if(valueObj instanceof Value value){
                        targetValue=value;
                    }else if(valueObj instanceof Collection collectionValue){
                       for(Object item : collectionValue){
                           if(item instanceof Value value){
                               if(value.getName().equalsIgnoreCase(vName)){
                                   targetValue=value;
                                   break;
                               }
                           }
                       }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return targetValue;
    }

    /**
     * 获取所有引用其他节点值的变量
     * @param nodeMeta
     * @return
     */
    public static List<Value> getValuesToResolveRef(NodeMeta nodeMeta) {
        // 获取所有字段
        List<Field> fields = getAllFields(nodeMeta.getClass());
        if (fields.isEmpty()) {
            return Collections.emptyList();
        }

        // 过滤带有 @Before 注解的字段
        return fields.stream()
                .filter(field -> field.getAnnotation(DependsRef.class) != null)
                .map(field -> {
                    try {
                        field.setAccessible(true); // 允许访问私有字段
                        return field.get(nodeMeta); // 获取字段值
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(Objects::nonNull) // 过滤空值
                .flatMap(value -> {
                    // 判断字段值的类型
                    if (value instanceof Collection) {
                        // 如果是集合类型，遍历集合中的元素，并且只保留 Value 类型的元素
                        return ((Collection<?>) value).stream()
                                .filter(item -> item instanceof Value)
                                .map(item->{
                                    return (Value)item;
                                });
                    } else if (value instanceof Value) {
                        // 如果是 Value 类型，直接返回
                        return Stream.of((Value)value);
                    } else if (value instanceof Integer) {
                        // 如果是 int 类型，忽略
                        return Stream.empty();
                    } else {
                        // 其他类型，直接返回
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());
    }
    /**
     * 获取所有引用其他节点值的变量，并转为数组
     * @param nodeMeta
     * @return
     */
    public static Map<String,Value> getValuesToResolveRefMap(NodeMeta nodeMeta){
        List<Value> values= getValuesToResolveRef(nodeMeta);
        Map<String,Value> valueMap=new HashMap<>();
        if(ObjectUtil.isEmpty(values)){
            return valueMap;
        }
        values.forEach(item->{
            valueMap.put(item.getName(),item);
        });
        return valueMap;
    }

    /**
     * 获取所有内容含有变量的变量
     * @param nodeMeta
     * @return
     */
    public static List<Field> getValuesToResolveVariable(NodeMeta nodeMeta) {
        // 获取所有字段
        List<Field> fields = getAllFields(nodeMeta.getClass());
        if (fields.isEmpty()) {
            return Collections.emptyList();
        }

        // 过滤带有 @Before 注解的字段
        fields= fields.stream()
                .filter(field -> field.getAnnotation(DependsVariable.class) != null)
                .toList();

        if(ObjectUtil.isEmpty(fields)){
            return Collections.emptyList();
        }
        fields.forEach(item->{
            item.setAccessible(true);
        });

        return fields;
    }



    // 获取所有字段，包括父类字段，但不包括 Object 类
    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            Field[] declaredFields = clazz.getDeclaredFields();
            fields.addAll(Arrays.asList(declaredFields));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }
}
