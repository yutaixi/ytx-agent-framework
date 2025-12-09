package com.ytx.ai.workflow.node;

import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.execute.FlowContext;

public interface WorkflowNode {

    public String getType();

    public NodeOutput run(FlowNode flowNode, FlowContext flowContext);

    public NodeOutput doBiz(FlowNode flowNode, FlowContext flowContext);

    public Class<? extends NodeMeta> getMetaClass();
}
