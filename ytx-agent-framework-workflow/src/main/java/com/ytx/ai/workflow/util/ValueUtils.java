package com.ytx.ai.workflow.util;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.NodeResult;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.ValueSource;
import com.ytx.ai.workflow.enums.SystemVariableEnum;
import com.ytx.ai.workflow.enums.ValueSourceTypeEnum;
import com.ytx.ai.workflow.enums.ValueTypeEnum;
import com.ytx.ai.workflow.execute.FlowContext;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
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
            case ARRAY_STRING:
            case ARRAY_BOOLEAN:
            case ARRAY_NUMBER:
            case ARRAY_INTEGER:
            case ARRAY_TIME:
            case ARRAY_OBJECT:
                result = (T) value.getContent();
                break;

            default:
                result = (T) value.getContent();
                break;
        }

        return result;
    }


    public static String getValueType(Object value){

        if(value instanceof Number){
            return ValueTypeEnum.NUMBER.getType();
        }
        if(value instanceof String){
            return ValueTypeEnum.STRING.getType();
        }
        if(value instanceof Boolean){
            return ValueTypeEnum.BOOLEAN.getType();
        }
        if(value instanceof Date){
            return ValueTypeEnum.TIME.getType();
        }
        if(value instanceof Collection<?> collection){
            if(collection.isEmpty()){
                return ValueTypeEnum.ARRAY_STRING.getType();
            }
            Object firstItem=collection.iterator().next();
            if(firstItem instanceof String){
                return ValueTypeEnum.ARRAY_STRING.getType();
            }
            if(firstItem instanceof Integer){
                return ValueTypeEnum.ARRAY_INTEGER.getType();
            }
            if(firstItem instanceof Boolean){
                return ValueTypeEnum.ARRAY_BOOLEAN.getType();
            }
            if(firstItem instanceof Number){
                return ValueTypeEnum.ARRAY_NUMBER.getType();
            }
            if(firstItem instanceof Date){
                return ValueTypeEnum.ARRAY_TIME.getType();
            }
            return ValueTypeEnum.ARRAY_STRING.getType();
        }
        return ValueTypeEnum.OBJECT.getType();
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
            NodeResult refNodeResult = flowContext.getNodeOutputMap().get(valueSource.getNId());
            if (refNodeResult == null || ObjectUtil.isEmpty(refNodeResult.getNodeMeta())) {
                return;
            }

            NodeMeta nodeMeta = refNodeResult.getNodeMeta();

            String vName = valueSource.getVName();
            String path = null;

            // 处理嵌套引用：检查变量名是否包含点号，如 "user.name"
            if (StrUtil.isNotEmpty(vName) && vName.contains(".")) {
                int firstDotIndex = vName.indexOf(".");
                // 截取根变量名，如 "user"
                String rootName = vName.substring(0, firstDotIndex);
                // 截取路径，如 "name"
                path = vName.substring(firstDotIndex + 1);
                // 更新vName为根变量名，以便获取根Value对象
                vName = rootName;
            }

            Value refValue = NodeReflectUtils.getValue(nodeMeta, vName, valueSource.getVGroup());
            if (ObjectUtil.isEmpty(refValue)) {
                return;
            }

            if (StrUtil.isNotEmpty(path)) {
                // 如果存在路径，则从根Value的内容中递归获取子属性值
                Object content = refValue.getContent();
                Object childValue = getValueByPath(content, path);
                value.setContent(childValue);
            } else {
                value.setContent(refValue.getContent());
                value.setSource(refValue.getSource());
            }
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
     * 处理变量集,变量格式格式{{xxx}}，支持{{xxx.a.b.c}}格式
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

        String orgValueContent;
        if(value instanceof Value val){
            orgValueContent= String.valueOf(val.getContent()) ;
        }else if(value instanceof String valueStr){
            orgValueContent=valueStr;
        }else {
            return;
        }

        if(ObjectUtil.isEmpty(orgValueContent) ){
            return;
        }
        AtomicReference<String> valueContent=new AtomicReference<>(orgValueContent);

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
            if(value instanceof Value val){
                val.setContent(valueContent.get());
            }else if(value instanceof String valueStr){
                field.set(nodeMeta,valueContent.get());
            }
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
        if(ObjectUtil.isEmpty(variable)){
            return "";
        }
        variable=variable.trim();
        // 1. 尝试直接获取变量值
        Object variableValue = getDirectVariableValue(variable, inputs, flowContext);
        if(ObjectUtil.isNotEmpty(variableValue)){
            return variableValue;
        }
        if(variable.endsWith(".")){
            variable=variable.substring(0,variable.length()-1);
        }
        if(!variable.contains(".")){
            return "";
        }
        // 2. 如果直接获取不到，且变量名包含点号，尝试解析嵌套属性
        int firstDotIndex = variable.indexOf(".");
        String rootVariable = variable.substring(0, firstDotIndex);
        String path = variable.substring(firstDotIndex + 1);

        Object rootValue = getDirectVariableValue(rootVariable, inputs, flowContext);
        if (rootValue != null) {
            variableValue = getValueByPath(rootValue, path);
        }
        return variableValue;
    }

    /**
     * 直接获取变量值（不处理嵌套属性）
     *
     * @param variable    变量名称
     * @param inputs      输入参数
     * @param flowContext 流程上下文
     * @return 变量值
     */
    private static Object getDirectVariableValue(String variable, Map<String, Value> inputs, FlowContext flowContext) {
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
     * 通过路径获取对象属性值
     * <p>
     * 功能说明：
     * 根据点号分隔的路径，从根对象中逐层获取属性值。
     * 支持多种数据结构的处理，包括Map、List、数组和普通POJO对象。
     * </p>
     *
     * @param root 根对象，数据源
     * @param path 属性路径，例如 "user.address.city" 或 "items.0.name"
     * @return 对应路径的属性值，如果路径不存在或中间值为null，则返回null
     */
    private static Object getValueByPath(Object root, String path) {
        if (root == null) {
            return null;
        }
        if(StrUtil.isBlank(path)){
            return root;
        }
        String[] keys = path.split("\\.");
        Object current = root;
        for (String key : keys) {
            if (current == null) {
                return null;
            }
            current = getChildValue(current, key);
        }
        return current;
    }

    /**
     * 获取对象的子属性值
     * <p>
     * 实现逻辑：
     * 1. 如果是 Map 或 JSONObject，通过 key 获取值
     * 2. 如果是 List 或 JSONArray，尝试将 key 转为索引获取值
     * 3. 如果是 数组，尝试将 key 转为索引获取值
     * 4. 如果是 普通对象，通过反射获取字段值
     * </p>
     *
     * @param current 当前对象
     * @param key     属性名或索引
     * @return 子属性值
     */
    private static Object getChildValue(Object current, String key) {
        // 1. Map 类型 (包括 Hutool JSONObject)
        if (current instanceof Map) {
            return ((Map<?, ?>) current).get(key);
        }
        // 2. List 类型 (包括 Hutool JSONArray)
        if (current instanceof List) {
            return getListElement((List<?>) current, key);
        }
        // 3. 数组 类型
        if (current.getClass().isArray()) {
            return getArrayElement(current, key);
        }
        // 4. 普通对象 (使用反射)
        try {
            return ReflectUtil.getFieldValue(current, key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 List 元素
     *
     * @param list 列表
     * @param key  索引
     * @return 元素值
     */
    private static Object getListElement(List<?> list, String key) {
        try {
            int index = Integer.parseInt(key);
            if (index >= 0 && index < list.size()) {
                return list.get(index);
            }
        } catch (NumberFormatException e) {
            // ignore
        }
        return null;
    }

    /**
     * 获取数组元素
     *
     * @param array 数组对象
     * @param key   索引
     * @return 元素值
     */
    private static Object getArrayElement(Object array, String key) {
        try {
            int index = Integer.parseInt(key);
            int length = Array.getLength(array);
            if (index >= 0 && index < length) {
                return Array.get(array, index);
            }
        } catch (NumberFormatException e) {
            // ignore
        }
        return null;
    }

    /**
     * 替换变量值
     * @param value         值对象
     * @param variableName  变量名称，
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


    public static void expandInputs(NodeMeta nodeMeta, FlowContext flowContext){

        if(ObjectUtil.isEmpty(nodeMeta.getInputs())){
            return;
        }
        Map<String,Value> valueMap=toMap(nodeMeta.getInputs());
        List<Field> fields=NodeReflectUtils.getExpandInputsFields(nodeMeta);
        for(Field field:fields){
            String name=field.getName();
            Value value=valueMap.get(name);
            if(ObjectUtil.isNotEmpty(value)){
                try {
                    field.set(nodeMeta,value.getContent());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void contractOutputs(NodeMeta nodeMeta, FlowContext flowContext){
        List<Field> fields=NodeReflectUtils.getContractOutputsFields(nodeMeta);
        if(ObjectUtil.isEmpty(fields)){
            return;
        }
        Map<String,Value> outputValueMap=new HashMap<>();
        if(ObjectUtil.isNotEmpty(nodeMeta.getOutputs())){
            outputValueMap=toMap(nodeMeta.getOutputs());
        }
        for(Field field:fields){
            String name=field.getName();
            try {
                Object value=field.get(nodeMeta);

                Value outputValue=outputValueMap.computeIfAbsent(name,item->Value.builder().name(name).build());
                outputValue.setContent(value);
                outputValue.setType(getValueType(value));

            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        nodeMeta.setOutputs(outputValueMap.values().stream().toList());
    }


    /**
     * 将节点执行的结果映射到输出结果中。
     * <p>
     * 功能说明：根据 outputs 中定义的属性名（如 name、age、country），从 result 中取同名字段的值并写入对应 Value。
     * 适用于大模型以 JSON 格式输出时，将 JSON 属性与界面定义的输出属性一一对应。
     * </p>
     *
     * @param result  节点执行结果，支持 JSONObject 或 String 类型。为 String 时仅当可解析为 JSON 对象时才进行映射
     * @param outputs 输出定义列表，每个 Value 的 name 对应 JSON 中的属性名
     */
    public static void result2Outputs(Object result, List<Value> outputs) {
        if (ObjectUtil.isEmpty(outputs)) {
            return;
        }
        JSONObject jsonObject = toJSONObject(result);
        if (jsonObject == null) {
            return;
        }
        Map<String, Value> outputsMap = new HashMap<>();
        outputs.forEach(item -> outputsMap.put(item.getName(), item));

        outputsMap.forEach((key, outputValue) -> {
            Object value = jsonObject.get(key);
            if (outputValue != null) {
                outputValue.setContent(value);
            }
        });
    }

    /**
     * 将结果对象转换为 JSONObject。
     * <p>
     * 转换规则：
     * 1. 如果 result 已经是 JSONObject，直接返回
     * 2. 如果 result 是 String，尝试解析为 JSON 对象；非 JSON 或解析失败返回 null
     * 3. 其他类型返回 null
     * </p>
     *
     * @param result 待转换的结果对象
     * @return JSONObject 对象，无法转换时返回 null
     */
    private static JSONObject toJSONObject(Object result) {
        if (result instanceof JSONObject) {
            return (JSONObject) result;
        }
        if (result instanceof String strValue) {
            if (StrUtil.isBlank(strValue) || !JSONUtil.isTypeJSON(strValue)) {
                return null;
            }
            try {
                return JSONUtil.parseObj(strValue);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

}

