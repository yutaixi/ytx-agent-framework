package com.ytx.ai.workflow.node.internal;

import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.node.BasicNode;
import com.ytx.ai.workflow.node.NodeOutput;
import lombok.Getter;
import lombok.Setter;

/**
 * Batch body node for batch processing body
 * 批量处理体节点
 */
public class BatchBodyNode extends BasicNode {

    /**
     * 初始化方法
     * 可根据需要扩展初始化逻辑
     */
    @Override
    public void init() {
    }

    /**
     * 获取节点类型
     *
     * @return 节点类型字符串
     */
    @Override
    public String getType() {
        return WorkflowPluginTypeIdEnum.BATCH_BODY.getType();
    }

    /**
     * 执行节点业务逻辑
     *
     * @param flowNode 流程节点
     * @param flowContext 流程上下文
     * @return 节点输出结果
     */
    @Override
    public NodeOutput doBiz(FlowNode flowNode, FlowContext flowContext) {
        return null;
    }

    /**
     * 获取元数据类
     *
     * @return 元数据类的Class对象
     */
    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return BatchBodyNodeMeta.class;
    }

    /**
     * Batch body node metadata
     * 批量处理体节点元数据
     */
    @Getter
    @Setter
    public static class BatchBodyNodeMeta implements NodeMeta {
        /**
         * Inner workflow definition
         * 内嵌工作流定义（可以是 JSON 对象或其他格式）
         */
        private Object innerWorkflow;
    }
}

