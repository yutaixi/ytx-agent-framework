package com.ytx.ai.workflow.util;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.annotation.DependsRef;
import com.ytx.ai.workflow.annotation.DependsVariable;
import com.ytx.ai.workflow.enums.ValueSourceTypeEnum;

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


    public static List<Value> getValuesToResolveRef(NodeMeta nodeMeta) {
        List<Value> result = new ArrayList<>();
        Deque<Object> stack = new ArrayDeque<>();
        Set<Object> processed = new HashSet<>();

        // 初始压入NodeMeta对象
        stack.push(nodeMeta);

        while (!stack.isEmpty()) {
            Object current = stack.pop();
            if (current == null || processed.contains(current)) {
                continue;
            }
            processed.add(current);

            List<Field> fields = getAllFields(current.getClass());

            fields.stream()
                    .filter(field -> field.getAnnotation(DependsRef.class) != null)
                    .forEach(field -> {
                        try {
                            field.setAccessible(true);
                            Object value = field.get(current);

                            if (value instanceof Collection) {
                                ((Collection<?>) value).forEach(item -> {
                                    if (item instanceof Value) {
                                        result.add((Value) item);
                                    }
                                    // 压入集合元素进行深度处理
                                    stack.push(item);
                                });
                            } else if (value instanceof Value) {
                                result.add((Value) value);
                                // 压入Value对象进行深度处理
                                stack.push(value);
                            } else if (value != null) {
                                // 压入普通对象进行深度处理
                                stack.push(value);
                            }
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

//        return result.stream().filter(item->{
//            return ObjectUtil.isNotEmpty(item.getSource()) && ObjectUtil.equals(ValueSourceTypeEnum.REFERENCE.getSource(),item.getSource().getType());
//        }).collect(Collectors.toList());

        return result;
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
