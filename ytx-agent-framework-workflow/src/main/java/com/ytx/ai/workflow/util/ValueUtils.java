package com.ytx.ai.workflow.util;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.NodeOutput;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.ValueSource;
import com.ytx.ai.workflow.enums.SystemVariableEnum;
import com.ytx.ai.workflow.enums.ValueSourceTypeEnum;
import com.ytx.ai.workflow.enums.ValueTypeEnum;
import com.ytx.ai.workflow.execute.FlowContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValueUtils {

    public static <T> T getValue(Value value) {
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }

        ValueTypeEnum typeEnum = ValueTypeEnum.of(value.getType());
        if(ObjectUtil.isEmpty(typeEnum)){
            typeEnum=ValueTypeEnum.STRING;
        }
        T result = null;

        switch (typeEnum) {
            case NUMBER:
                result = (T) Double.valueOf(value.getContent().toString());
                break;

            case STRING:
            case TIME:
            case BOOLEAN:
            case OBJECT:
            case INTEGER:
                result = (T) value.getContent();
                break;

            default:
                result = (T) value.getContent();
                break;
        }

        return result;
    }

    public static <T> T getValue(String name, Map<String, Value> dataMap) {
        Value value = dataMap.get(name);
        return getValue(value);
    }

    public static List<String> findReferenceVariable(String content) {
        // 正则表达式匹配{{xxx}}格式
        String regex = "\\{\\{(.*?)\\}\\}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        List<String> referenceVariables = new ArrayList<>();
        while (matcher.find()) {
            referenceVariables.add(matcher.group(1));
        }
        return referenceVariables;
    }

    /**
     * 处理引用的数据集
     * @param param
     * @param flowContext
     */
    public static void resolveRefValues(Map<String, Value> param, FlowContext flowContext) {
        if (ObjectUtil.isEmpty(param)) {
            return;
        }

        param.forEach((k, v) -> {
            // 参数值引用其他节点
            resolveRefValue(v, flowContext);
        });
    }

    /**
     * 处理引用的数据集
     * @param nodeMeta
     * @param flowContext
     */
    public static void resolveRefValues(NodeMeta nodeMeta, FlowContext flowContext) {
        if (ObjectUtil.isEmpty(nodeMeta)) {
            return;
        }
        List<Value> values=NodeReflectUtils.getValuesToResolveRef(nodeMeta);

        if(ObjectUtil.isEmpty(values)){
            return;
        }
        values.forEach(value->{
            // 参数值引用其他节点
            resolveRefValue(value, flowContext);
        });



    }

    /**
     * 处理引用的数据
     *
     * @param value
     * @param flowContext
     */
    public static void resolveRefValue(Value value, FlowContext flowContext) {
        if (ObjectUtil.isEmpty(value)) {
            return;
        }

        // 参数值引用其他节点
        ValueSource valueSource = value.getSource();
        if (valueSource != null && ValueSourceTypeEnum.isReference(valueSource.getType())) {
            NodeOutput refNodeOutput = flowContext.getNodeOutputMap().get(valueSource.getNId());
            if (refNodeOutput == null || ObjectUtil.isEmpty(refNodeOutput.getNodeMeta())) {
                return;
            }

            NodeMeta nodeMeta=refNodeOutput.getNodeMeta();
            Value refValue = NodeReflectUtils.getValue(nodeMeta,valueSource.getVName(),valueSource.getVGroup());
            if (ObjectUtil.isEmpty(refValue)) {
                return;
            }

            value.setContent(refValue.getContent());
            value.setSource(refValue.getSource());
        }
    }

    /**
     * 处理变量集
     * @param param       参数集合
     * @param inputs      输入集合
     * @param flowContext 流程上下文
     */
    public static void resolveVariables(Map<String, Value> param, Map<String, Value> inputs, FlowContext flowContext) {
        if (ObjectUtil.isEmpty(param)) {
            return;
        }

        param.forEach((k, v) -> {
            // 参数值引用其他节点
            resolveVariable(v, inputs, flowContext);
        });
    }

    /**
     * 处理变量集
     * @param nodeMeta    节点元数据
     * @param flowContext 流程上下文
     */
    public static void resolveVariables(NodeMeta nodeMeta, FlowContext flowContext) {
        if (ObjectUtil.isEmpty(nodeMeta)) {
            return;
        }

        List<Field> fields=NodeReflectUtils.getValuesToResolveVariable(nodeMeta);
        Map<String,Value> valueMap= NodeReflectUtils.getValuesToResolveRefMap(nodeMeta);
        fields.forEach((field) -> {
            // 参数值引用其他节点
            resolveVariable(field,nodeMeta, valueMap, flowContext);
        });
    }

    /**
     * 处理变量
     *
     * @param value
     * @param inputs
     * @param flowContext
     */
    public static void resolveVariable(Value value, Map<String, Value> inputs, FlowContext flowContext) {
        if (ObjectUtil.isEmpty(value)) {
            return;
        }

        // 获取来源信息
        ValueSource valueSource = value.getSource();
        // 如果是Ref类型则跳过，只处理Literal中的变量
        if (valueSource != null && !ValueSourceTypeEnum.isLiteral(valueSource.getType())) {
            return;
        }

        Object valueContent = value.getContent();
        // 原始content为空，则没有变量，不需要处理
        if (ObjectUtil.isEmpty(valueContent)) {
            return;
        }

        if (valueContent instanceof String content) {
            // 找到变量
            List<String> variables = findReferenceVariable(content);
            if (ObjectUtil.isEmpty(variables)) {
                return;
            }

            variables.forEach(variable -> {
                // 获取变量值
                Object variableValue = getVariableValue(variable, inputs, flowContext);
                // 替换变量值
                replaceVariableValue(value, variable, variableValue);
            });
        }
    }
    /**
     * 处理变量
     *
     * @param field
     * @param values
     * @param flowContext
     */
    public static void resolveVariable(Field field,NodeMeta nodeMeta, Map<String, Value> values, FlowContext flowContext) {

        // 原始content为空，则没有变量，不需要处理
        if (ObjectUtil.isEmpty(field)) {
            return ;
        }
        Object value=null;
        try {
            value=field.get(nodeMeta);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        if(ObjectUtil.isEmpty(value) || ObjectUtil.isEmpty(value.toString())){
            return;
        }
        AtomicReference<String> valueContent=new AtomicReference<>(value.toString());

        // 找到变量
        List<String> variables = findReferenceVariable(valueContent.get());
        if (ObjectUtil.isEmpty(variables)) {
            return ;
        }

        variables.forEach(variable -> {
            // 获取变量值
            Object variableValue = getVariableValue(variable, values, flowContext);
            if(variableValue==null){
                return;
            }
            // 替换变量值
            valueContent.set(replaceVariableValue(valueContent.get(), variable, variableValue));
        });

        try {
            field.set(nodeMeta,valueContent.get());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 获取变量的值
     *
     * @param variable    变量名称
     * @param inputs      输入参数
     * @param flowContext 流程上下文
     * @return 返回变量的值
     */
    private static Object getVariableValue(String variable, Map<String, Value> inputs, FlowContext flowContext) {
        Object variableValue = null;

        // 处理系统预置变量
        if (SystemVariableEnum.isSystemVariable(variable)) {
            SystemVariableEnum systemVariable = SystemVariableEnum.of(variable);
            switch (systemVariable) {
                case MEMORY:
                    variableValue = flowContext.getMemoryStr();
                    break;
                case USER_INPUT:
                    if (ObjectUtil.isNotEmpty(flowContext.getChat())) {
                        variableValue = flowContext.getChat().getQuestion();
                    }
                    break;
                case CHAT_HISTORY:
                    if (ObjectUtil.isNotEmpty(flowContext.getChat())) {
                        variableValue = flowContext.getChat().getHistory();
                    }
                    break;
                case null:
                    break;
            }
        } else {
            // 处理用户自定义变量
            if (ObjectUtil.isNotEmpty(inputs)) {
                Value refValue = inputs.get(variable);
                if (ObjectUtil.isNotEmpty(refValue)) {
                    variableValue = refValue.getContent();
                }
            }
        }

        return variableValue;
    }

    /**
     * 替换变量值
     *
     * @param value         值对象
     * @param variableName  变量名称
     * @param content       替换内容
     */
    private static void replaceVariableValue(Value value, String variableName, Object content) {
        Object orgContent = value.getContent();
        if (orgContent instanceof String orgContentStr) {
            String variableSymbol = formatVariableSymbol(variableName);
            if (orgContentStr.equalsIgnoreCase(variableSymbol)) {
                value.setContent(content);
            } else {
                String valueStr = content == null ? "" : content.toString();
                value.setContent(orgContentStr.replace(variableSymbol, valueStr));
            }
        }
    }

    /**
     * 替换变量值
     *
     * @param orgContent     原始内容
     * @param variableName  变量名称
     * @param content       替换内容
     */
    private static String replaceVariableValue(String orgContent, String variableName, Object content) {
        String resultContent=orgContent;
            String variableSymbol = formatVariableSymbol(variableName);
            if (orgContent.equalsIgnoreCase(variableSymbol)) {
                resultContent=content.toString();
            } else {
                String valueStr = content == null ? "" : content.toString();
                resultContent=orgContent.replace(variableSymbol, valueStr);
            }
        return resultContent;
    }

    private static String formatVariableSymbol(String variable) {
        return "{{" + variable + "}}";
    }


    public static Map<String,Value> toMap(List<Value> values){

        Map<String,Value> valueMap=new HashMap<>();
        if(ObjectUtil.isEmpty(values)){
            return valueMap;
        }
        values.forEach(item->{
            valueMap.put(item.getName(),item);
        });
        return valueMap;
    }

}
