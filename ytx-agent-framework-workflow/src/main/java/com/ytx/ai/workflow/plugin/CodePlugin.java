//package com.ytx.ai.workflow.plugin;
//
//import cn.hutool.core.util.ObjectUtil;
//import com.ytx.ai.sandbox.Args;
//import com.ytx.ai.sandbox.Output;
//import com.ytx.ai.sandbox.runtime.Sandbox;
//import com.ytx.ai.workflow.FlowNode;
//import com.ytx.ai.workflow.NodeMeta;
//import com.ytx.ai.workflow.Value;
//import com.ytx.ai.workflow.enums.PluginTypeIdEnum;
//import com.ytx.ai.workflow.enums.ValueTypeEnum;
//import com.ytx.ai.workflow.execute.FlowContext;
//import com.ytx.ai.workflow.util.ValueUtils;
//
//import java.util.Map;
//
//public class CodePlugin extends BasicPlugin {
//
//    private static final String CODE_SCRIPT = "code_script";
//    private static final String CODE_LANGUAGE = "code_language";
//
//    @Override
//    public String getType() {
//        return PluginTypeIdEnum.CODE.getType();
//    }
//
//    @Override
//    public PluginOutput doBiz(FlowNode flowNode, FlowContext flowContext) {
//        String script = ValueUtils.getValue(CODE_SCRIPT, flowNode.getMetaData());
//        String language = ValueUtils.getValue(CODE_LANGUAGE, flowNode.getMetaData());
//        Output sandBoxOutput = null;
//
//        Map<String, Value> inputMap = flowNode.getInputs();
//        if (ObjectUtil.isNotEmpty(inputMap)) {
//            Args args = new Args();
//            inputMap.forEach((key, value) -> args.bind(key, ValueUtils.getValue(value)));
//            sandBoxOutput = Sandbox.run(language, script, args);
//        } else {
//            sandBoxOutput = Sandbox.run(language, script);
//        }
//
//        PluginOutput output = PluginOutput.of();
//        output.setMetaResult(sandBoxOutput.getValueMap());
//        return output;
//    }
//
//    @Override
//    public Class<? extends NodeMeta> getMetaClass() {
//        return null;
//    }
//
//    @Override
//    public void init() {
//        inputDefinition.add(Param.builder().name(CODE_SCRIPT).type(ValueTypeEnum.STRING).required(true).build());
//        inputDefinition.add(Param.builder().name(CODE_LANGUAGE).type(ValueTypeEnum.STRING).required(true).build());
//    }
//}
