package com.ytx.ai.workflow;

import com.ytx.ai.workflow.enums.ComponentTypeEnum;
import com.ytx.ai.workflow.util.NodeReflectUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class FlowNode {

    private String id;

    /** 节点类型 */
    private ComponentTypeEnum componentType;

    /** 组件ID */
    private String componentId;

    /** 节点显示名称 */
    private String label;

    private NodeMeta meta;

    public boolean isStartNode() {
        return ComponentTypeEnum.plugin.equals(componentType)
                && NodeReflectUtils.isStartNode(this);
    }

    public boolean isEndNode() {
        return ComponentTypeEnum.plugin.equals(componentType)
                && NodeReflectUtils.isEndNode(this);
    }
}
