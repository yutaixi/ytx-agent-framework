package com.ytx.ai.workflow.adaptor.processor;

import com.ytx.ai.workflow.Workflow;

/**
 * Workflow parse post processor interface
 * 工作流解析后处理器接口
 * 用于在工作流解析完成后进行额外的处理逻辑
 */
public interface WorkflowParsePostProcessor {

    /**
     * Process the workflow after parsing
     * 在工作流解析完成后进行处理
     *
     * @param workflow 待处理的工作流对象
     */
    void process(Workflow workflow);
}
