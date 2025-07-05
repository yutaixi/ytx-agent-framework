package com.ytx.ai.workflow.plugin;

import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.execute.FlowContext;

public interface Plugin {

    public String getType();

    public PluginOutput run(FlowNode flowNode, FlowContext flowContext);

    public PluginOutput doBiz(FlowNode flowNode, FlowContext flowContext);

    public Class<? extends NodeMeta> getMetaClass();
}
