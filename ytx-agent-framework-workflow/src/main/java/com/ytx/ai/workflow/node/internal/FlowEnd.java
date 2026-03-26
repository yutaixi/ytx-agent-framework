package com.ytx.ai.workflow.node.internal;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.agent.llm.callback.StreamCallback;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.annotation.DependsRef;
import com.ytx.ai.workflow.annotation.DependsVariable;
import com.ytx.ai.workflow.annotation.EndNode;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.node.BasicNode;
import com.ytx.ai.workflow.node.NodeOutput;
import com.ytx.ai.workflow.util.ValueUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class FlowEnd extends BasicNode {

    @Override
    public void init() {
    }

    @Override
    public String getType() {
        return WorkflowPluginTypeIdEnum.END.getType();
    }

    /**
     * 结束节点执行逻辑。
     * <p>
     * 分两个分支处理：
     * <ul>
     *   <li>非流式输出（streamOutput=false）：将输出变量和回答文本写入 NodeOutput，
     *       由调用方将结果序列化后一次性返回给前端。</li>
     *   <li>流式输出（streamOutput=true）：在完成常规输出写入的同时，
     *       通过 {@link StreamCallback#onWorkflowCompleted(String)} 通知上层"工作流已执行完成"。
     *       增量内容已由紧邻的 LLM 节点逐块推送，此处仅发出流式结束信号。</li>
     * </ul>
     *
     * @param flowNode    当前节点定义，含节点元数据
     * @param flowContext 工作流执行上下文，含流式回调等运行时信息
     * @return NodeOutput 节点输出（包含输出变量 map 和回答文本）
     */
    @Override
    public NodeOutput doBiz(FlowNode flowNode, FlowContext flowContext) {

        EndNodeMeta nodeMeta = (EndNodeMeta) flowNode.getMeta();

        NodeOutput output = NodeOutput.of();
        output.setData(ValueUtils.toMap(nodeMeta.getOutputs()));

        String outputText = nodeMeta.getOutputText();
        if (ObjectUtil.isNotEmpty(outputText)) {
            output.setAnswer(outputText);
        }

        // 流式输出分支：当 streamOutput=true 且上下文携带流式回调时，
        // 通知上层工作流结束（实际增量内容由 LLM 节点逐块推送，此处只发出完成信号）。
        // 流式 LLM 模式下 outputText 通常为空字符串；非流式 LLM 模式下为完整回答文本。
        if (nodeMeta.isStreamOutput()) {
            StreamCallback streamCallback = flowContext.getStreamCallback();
            if (streamCallback != null) {
                streamCallback.onWorkflowCompleted(outputText != null ? outputText : "");
            }
        }

        return output;
    }

    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return EndNodeMeta.class;
    }


    @Getter
    @Setter
    @EndNode
    public static class EndNodeMeta implements NodeMeta {

        /** 返回类型（returnVariable / returnText） */
        private String returnType;

        /** 输出变量列表，引用上游节点的输出 */
        @DependsRef
        private List<Value> outputs;

        /** 回答文本模板，支持变量引用（如 {{output}}），流式模式下解析后通常为空 */
        @DependsVariable
        private String outputText;

        /** 是否开启流式输出，对应前端 End 节点的"流式输出"开关 */
        private boolean streamOutput;
    }
}