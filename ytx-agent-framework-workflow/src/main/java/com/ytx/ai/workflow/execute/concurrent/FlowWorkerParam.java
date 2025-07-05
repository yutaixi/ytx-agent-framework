package com.ytx.ai.workflow.execute.concurrent;

import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.WorkflowOutput;
import com.ytx.ai.workflow.execute.FlowContext;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FlowWorkerParam {

    private FlowContext flowContext;

    private FlowNode flowNode;

    private WorkflowOutput workflowOutput;
}