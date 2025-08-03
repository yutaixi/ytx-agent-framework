package com.ytx.ai.workflow.plugin.flow;

import cn.hutool.core.util.ObjectUtil;
import com.plexpt.chatgpt.entity.chat.ResponseFormat;
import com.ytx.ai.agent.llm.service.LlmService;
import com.ytx.ai.agent.llm.vo.LlmChatCompletion;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.annotation.DependsRef;
import com.ytx.ai.workflow.annotation.DependsVariable;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.plugin.BasicPlugin;
import com.ytx.ai.workflow.plugin.PluginOutput;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class LlmPlugin extends BasicPlugin {

    @Autowired
    private LlmService llmService;

    @Override
    public void init() {
    }

    @Override
    public String getType() {
        return WorkflowPluginTypeIdEnum.LLM.getType();
    }

    @Override
    public PluginOutput doBiz(FlowNode flowNode, FlowContext flowContext) {

        LlmNodeMeta nodeMeta=(LlmNodeMeta)flowNode.getMeta();
        // 用户提示词
        LlmChatCompletion llmChatCompletion = LlmChatCompletion.builder()
                .systemPrompt(nodeMeta.getSystemPrompt())
                .userPrompt(nodeMeta.getUserPrompt())
                .temperature(nodeMeta.getTemperature())
                .model(nodeMeta.getModelCode())
                .build();

        if (ObjectUtil.isNotEmpty(nodeMeta.getResponseFormat()) ) {
            if ("json".equalsIgnoreCase(nodeMeta.getResponseFormat())) {
                llmChatCompletion.setResponseFormat(ResponseFormat.Type.JSON_OBJECT.getValue());
            }
        }

        String content = llmService.chatCompletion(llmChatCompletion);
        Value outputValue=nodeMeta.getOutputs().getFirst();
        outputValue.setContent(content);
        return PluginOutput.of();
    }

    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return LlmNodeMeta.class;
    }


    @Getter
    @Setter
    public static class LlmNodeMeta implements NodeMeta {

        @DependsRef
        private List<Value> inputs;
        private List<Value> outputs;

        private String modelCode;
        @DependsVariable
        private String systemPrompt;
        @DependsVariable
        private String userPrompt;

        private Double temperature;
        private String responseFormat;

    }
}
