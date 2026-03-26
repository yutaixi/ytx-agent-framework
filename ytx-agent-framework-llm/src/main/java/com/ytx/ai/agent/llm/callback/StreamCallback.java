package com.ytx.ai.agent.llm.callback;

/**
 * 大模型流式对话回调接口。
 * <p>
 * 该接口用于在调用方与底层大模型服务之间传递流式增量结果，典型使用场景：
 * <ul>
 *     <li>在 {@code onDelta} 中将增量内容推送到工作流或 Web 层，实现前端的实时展示能力。</li>
 *     <li>在 {@code onCompleted} 中获取完整回复文本，用于后续结构化解析或写入工作流变量。</li>
 *     <li>在 {@code onError} 中统一处理模型调用异常，避免吞掉异常信息。</li>
 * </ul>
 */
public interface StreamCallback {

    /**
     * 接收一次大模型增量输出内容。
     *
     * @param deltaText 单次增量输出的文本内容，通常为追加片段，调用方需要自行决定如何拼接
     */
    void onDelta(String deltaText);

    /**
     * 流式输出完成后的回调。
     * <p>
     * 实现类应保证 fullText 是一个可直接用于后续业务处理的完整文本，例如：
     * <ul>
     *     <li>将 fullText 写入工作流节点的输出变量。</li>
     *     <li>作为最终回答返回给调用方。</li>
     * </ul>
     *
     * @param fullText 大模型本次对话的完整回复内容
     */
    void onCompleted(String fullText);

    /**
     * 流式调用过程中发生异常时的回调。
     *
     * @param throwable 大模型调用过程中抛出的异常实例
     */
    void onError(Throwable throwable);

    /**
     * 工作流流式输出完成时的回调，由 FlowEnd 节点（streamOutput=true）触发。
     * <p>
     * 与 {@link #onCompleted(String)} 的区别：
     * <ul>
     *     <li>{@code onCompleted} 表示单次 LLM 流式输出结束（由 LLM 节点内部触发）。</li>
     *     <li>{@code onWorkflowCompleted} 表示整个工作流结束节点执行完成（由 FlowEnd 节点触发）。</li>
     * </ul>
     * 默认实现为空操作，子类可覆盖以实现 SSE 完成信号等功能。
     *
     * @param answer 工作流最终输出的回答文本（流式模式下可能为空字符串）
     */
    default void onWorkflowCompleted(String answer) {
        // 默认空实现，子类按需覆盖
    }

}