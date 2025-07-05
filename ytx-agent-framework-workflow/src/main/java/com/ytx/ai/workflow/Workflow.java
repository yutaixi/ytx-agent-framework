package com.ytx.ai.workflow;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.agent.entity.SkillEntity;
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

    public static Workflow of(SkillEntity skillEntity) {
        Workflow workflow = Workflow.builder()
                .id(skillEntity.getId())
                .name(skillEntity.getName())
                .description(skillEntity.getDescription())
                .build();

        switch (skillEntity.getType()) {
            case "workflow":
                if (ObjectUtil.isEmpty(skillEntity.getDefinition())) {
                    break;
                }
                workflow=WorkflowAdaptor.parse(skillEntity);
                break;

            case "native":
                break;

            default:
                break;
        }

        return workflow;
    }
}
