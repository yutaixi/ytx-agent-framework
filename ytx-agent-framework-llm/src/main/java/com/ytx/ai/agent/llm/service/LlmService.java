package com.ytx.ai.agent.llm.service;


import com.ytx.ai.agent.llm.vo.ChatCompletionResponse;
import com.ytx.ai.agent.llm.vo.LlmChatCompletion;
import com.ytx.ai.agent.llm.callback.StreamCallback;

import java.util.List;

public interface LlmService {

    public ChatCompletionResponse chatCompletionWithResponse(LlmChatCompletion chatCompletion);

    public String chatCompletion(LlmChatCompletion llmChatCompletion);

    public List<Float> createEmbeddings(String content);

    /**
     * 使用流式方式执行大模型对话补全。
     * <p>
     * 实现要求：
     * <ul>
     *     <li>底层应调用模型提供方的流式 API（如 OpenAI Chat Completions 的 streaming 接口）。</li>
     *     <li>每收到一段增量内容时，通过 {@link StreamCallback#onDelta(String)} 回调通知上层。</li>
     *     <li>流式结束后，需要通过 {@link StreamCallback#onCompleted(String)} 返回完整回复文本。</li>
     *     <li>调用异常时必须回调 {@link StreamCallback#onError(Throwable)}，便于上层统一处理错误。</li>
     * </ul>
     *
     * @param chatCompletion 大模型对话补全请求参数，包含模型、提示词、上下文等信息
     * @param callback       流式回调接口，上层可在实现中将增量内容透传给工作流或 Web 层
     */
    public void chatCompletionStream(LlmChatCompletion chatCompletion, StreamCallback callback);
}