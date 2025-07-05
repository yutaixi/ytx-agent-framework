package com.ytx.ai.workflow;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FlowEdge {

    /** 唯一标识 */
    private String id;

    /** 开始节点 */
    private String source;

    /** 结束节点 */
    private String target;

    /** 依赖项 */
    private String depends;
}