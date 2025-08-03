package com.ytx.ai.workflow;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.base.workflow.Flow;
import com.ytx.ai.workflow.adaptor.WorkflowAdaptor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class Workflow {

    private Integer id;
    private String name;
    private String description;
    private List<FlowNode> nodes;
    private List<FlowEdge> edges;

    public static Workflow of(Flow flow) {
        Workflow workflow = Workflow.builder()
                .id(flow.getId())
                .name(flow.getName())
                .description(flow.getDescription())
                .build();

        switch (flow.getType()) {
            case "workflow":
                if (ObjectUtil.isEmpty(flow.getDefinition())) {
                    break;
                }
                workflow=WorkflowAdaptor.parse(flow);
                break;

            case "native":
                break;

            default:
                break;
        }

        return workflow;
    }
}
