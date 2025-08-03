package com.ytx.ai.workflow.plugin.flow;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.sandbox.Args;
import com.ytx.ai.sandbox.Output;
import com.ytx.ai.sandbox.runtime.Sandbox;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.annotation.DependsRef;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.plugin.BasicPlugin;
import com.ytx.ai.workflow.plugin.PluginOutput;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class CodePlugin extends BasicPlugin {

    @Override
    public String getType() {
        return WorkflowPluginTypeIdEnum.CODE.getType();
    }

    @Override
    public PluginOutput doBiz(FlowNode flowNode, FlowContext flowContext) {
        CodeNodeMeta codeNodeMeta=(CodeNodeMeta)flowNode.getMeta();
        String language=codeNodeMeta.getLanguage();
        if("javascript".equalsIgnoreCase(language)){
            language="js";
        }
        String script=codeNodeMeta.getScript();
        Output sandBoxOutput = null;

        List<Value> inputs = codeNodeMeta.getInputs();
        if (ObjectUtil.isNotEmpty(inputs)) {
            Args args = new Args();
            inputs.forEach(item -> args.bind(item.getName(), item.getContent()));
            sandBoxOutput = Sandbox.run(language, script, args);
        } else {
            sandBoxOutput = Sandbox.run(language, script);
        }

        List<Value> outputs= codeNodeMeta.getOutputs();
        if(ObjectUtil.isNotEmpty(outputs)){
            Output finalSandBoxOutput = sandBoxOutput;
            outputs.forEach(output->{
                Object value= finalSandBoxOutput.get(output.getName());
                output.setContent(value);
            });
        }

        PluginOutput output = PluginOutput.of();

        return output;
    }

    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return CodeNodeMeta.class;
    }

    @Getter
    @Setter
    public static class CodeNodeMeta implements NodeMeta {

        @DependsRef
        private List<Value> inputs;
        private List<Value> outputs;
        private String language;
        private String script;

    }

    @Override
    public void init() {
    }
}
