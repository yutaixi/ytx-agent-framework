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
import java.util.concurrent.atomic.AtomicReference;

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
            content = chatCompletionStreamWithCapture(llmChatCompletion, flowContext);
        } else {
            // 非流式调用，保持原有逻辑
            content = llmService.chatCompletion(llmChatCompletion);
        }
        // 将完整内容写入输出变量（流式与非流式统一处理，保证下游节点可正确引用 LLM 输出）
        if (isJsonResponse) {
            ValueUtils.result2Outputs(content, nodeMeta.getOutputs());
        } else {
            nodeMeta.getOutputs().forEach(item -> item.setContent(content));
        }
        return NodeOutput.of();
    }

    /**
     * 流式调用大模型，并捕获完整回复文本。
     * <p>
     * 设计说明：
     * <ul>
     *   <li>{@link LlmService#chatCompletionStream} 是同步阻塞调用，返回时流已全部接收完毕。</li>
     *   <li>完整文本在内部通过 {@link StreamCallback#onCompleted(String)} 回调传出，
     *       但调用方若只持有外部回调（如 SSE emitter）则无法拿到该文本。</li>
     *   <li>此方法构建一个透明包装回调：将所有事件原样转发给外部回调（保持 SSE 推送不变），
     *       同时用 {@link AtomicReference} 在 {@code onCompleted} 中捕获完整文本。</li>
     *   <li>流结束后返回该完整文本，供下游节点（Condition/Code/HTTP/FlowEnd 等）正确引用。</li>
     * </ul>
     *
     * @param llmChatCompletion 大模型对话请求参数
     * @param flowContext       工作流上下文，持有可选的外部流式回调（SSE emitter 等）
     * @return 大模型本次完整回复文本，若流式过程发生异常则返回空字符串
     */
    private String chatCompletionStreamWithCapture(LlmChatCompletion llmChatCompletion, FlowContext flowContext) {
        StreamCallback outerCallback = flowContext.getStreamCallback();
        // AtomicReference 存储 onCompleted 捕获到的完整文本
        AtomicReference<String> capturedTextRef = new AtomicReference<>("");

        // 透明包装回调：事件全部转发给外部回调，同时捕获 onCompleted 中的完整文本
        StreamCallback capturingCallback = new StreamCallback() {
            @Override
            public void onDelta(String deltaText) {
                if (outerCallback != null) {
                    outerCallback.onDelta(deltaText);
                }
            }

            @Override
            public void onCompleted(String fullText) {
                // 捕获完整文本，供 LlmNode 写入输出变量
                capturedTextRef.set(fullText != null ? fullText : "");
                if (outerCallback != null) {
                    outerCallback.onCompleted(fullText);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (outerCallback != null) {
                    outerCallback.onError(throwable);
                }
            }

            @Override
            public void onWorkflowCompleted(String answer) {
                if (outerCallback != null) {
                    outerCallback.onWorkflowCompleted(answer);
                }
            }
        };

        // chatCompletionStream 为同步阻塞调用，返回时 onCompleted 已被触发，capturedTextRef 已被赋值
        llmService.chatCompletionStream(llmChatCompletion, capturingCallback);
        return capturedTextRef.get();
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