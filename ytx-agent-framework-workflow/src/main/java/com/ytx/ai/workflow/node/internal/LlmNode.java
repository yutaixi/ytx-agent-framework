package com.ytx.ai.workflow.node.internal;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ytx.ai.agent.llm.callback.StreamCallback;
import com.ytx.ai.agent.llm.constants.ResponseFormatType;
import com.ytx.ai.agent.llm.service.LlmService;
import com.ytx.ai.agent.llm.vo.Content;
import com.ytx.ai.agent.llm.vo.LlmChatCompletion;
import com.ytx.ai.agent.llm.vo.Message;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.annotation.DependsRef;
import com.ytx.ai.workflow.annotation.DependsVariable;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.node.BasicNode;
import com.ytx.ai.workflow.node.NodeOutput;
import com.ytx.ai.workflow.util.ValueUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LlmNode extends BasicNode {

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
    public NodeOutput doBiz(FlowNode flowNode, FlowContext flowContext) {

        LlmNodeMeta nodeMeta=(LlmNodeMeta)flowNode.getMeta();

        List<Content> contents=new ArrayList<>();
        List<Message> history=new ArrayList<>();
        if(ObjectUtil.isNotEmpty(nodeMeta.getHistory()) && ObjectUtil.isNotEmpty(nodeMeta.getHistory().getContent()) ){
            if(nodeMeta.getHistory().getContent() instanceof Collection<?> historyCollection){
                historyCollection.forEach(message->{
                    history.add(JSONUtil.toBean(JSONUtil.toJsonStr(message),Message.class));
                });
            }
        }

        if(StrUtil.isNotBlank(nodeMeta.getUserPrompt())){
            Content content=Content.ofText(nodeMeta.getUserPrompt());
            contents.add(content);
        }
        if(ObjectUtil.isNotEmpty(nodeMeta.getFileInputs())){
            nodeMeta.getFileInputs().forEach(file->{
                if(ObjectUtil.isEmpty(file.getContent())){
                    return;
                }
                if(file.getContent() instanceof Collection<?> fileCollection){
                    fileCollection.forEach(item->{
                        Content content=Content.ofImage(String.valueOf(item));
                        contents.add(content);
                    });
                }else{
                    Content content=Content.ofImage(String.valueOf(file.getContent()));
                    contents.add(content);
                }
            });
        }
        // 用户提示词与上下文构建
        LlmChatCompletion llmChatCompletion = LlmChatCompletion.builder()
                .systemPrompt(nodeMeta.getSystemPrompt())
                .userPrompt(contents.toArray(Content[]::new))
                .history(history)
                .temperature(nodeMeta.getTemperature())
                .model(nodeMeta.getModelCode())
                .reasoningEffort(nodeMeta.getReasoningEffort())
                .build();

        if (ObjectUtil.isNotEmpty(nodeMeta.getResponseFormat()) ) {
            if ("json".equalsIgnoreCase(nodeMeta.getResponseFormat())) {
                llmChatCompletion.setResponseFormat(ResponseFormatType.JSON_OBJECT);
            }
        }
        boolean isJsonResponse = "json".equalsIgnoreCase(nodeMeta.getResponseFormat());
        boolean enableStreaming = nodeMeta.isStreaming();
        String content;
        if (enableStreaming) {
            // 流式调用大模型，在回调中将增量结果透传到 StreamCallback、
            StreamCallback sseStreamListener=flowContext.getStreamCallback();
            llmService.chatCompletionStream(llmChatCompletion,sseStreamListener );
            content="";
        } else {
            // 保持原有非流式调用逻辑，兼容历史行为
            content = llmService.chatCompletion(llmChatCompletion);
        }
        // 将最终完整内容写入输出变量，保持与历史行为一致
        if (isJsonResponse){
            ValueUtils.result2Outputs(content, nodeMeta.getOutputs());
        }else{
            nodeMeta.getOutputs().forEach(item->{
                item.setContent(content);
            });
        }
        return NodeOutput.of();
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
        @DependsRef
        private List<Value> fileInputs;
        @DependsRef
        private Value history;

        private String modelCode;
        private LlmChatCompletion.ReasoningEffort reasoningEffort;
        @DependsVariable
        private String systemPrompt;
        @DependsVariable
        private String userPrompt;

        private Double temperature;
        private String responseFormat;
        private boolean streaming;

    }
}
