package com.ytx.ai.workflow.plugin.flow;


import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.annotation.DependsVariable;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.plugin.BasicPlugin;
import com.ytx.ai.workflow.plugin.PluginOutput;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class HttpPlugin extends BasicPlugin {

    @Override
    public String getType() {
        return WorkflowPluginTypeIdEnum.HTTP.getType();
    }

    @Override
    public PluginOutput doBiz(FlowNode flowNode, FlowContext flowContext) {
        return null;
    }

    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return HttpPluginMeta.class;
    }

    @Override
    public void init() {

    }


    @Getter
    @Setter
    public static class HttpPluginMeta implements NodeMeta {

        //GET,POST,PUT,DELETE
        private String method;
        @DependsVariable
        private String url;
        private List<Value> requestParams;
        private List<Value> headers;
        private String bodyType;
        //当bodyType为binary时，body为base64编码的二进制
        private String body;
        private Integer retryCount;
        private Integer timeout;
        private List<Value> outputs;
        private String defaultOutput;

    }
}
