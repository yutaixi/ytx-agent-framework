package com.ytx.ai.workflow;

import com.ytx.ai.workflow.enums.ComponentTypeEnum;
import com.ytx.ai.workflow.enums.PluginTypeIdEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

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
                && PluginTypeIdEnum.START.getType().equalsIgnoreCase(componentId);
    }

    public boolean isEndNode() {
        return ComponentTypeEnum.plugin.equals(componentType)
                && PluginTypeIdEnum.END.getType().equalsIgnoreCase(componentId);
    }
}
