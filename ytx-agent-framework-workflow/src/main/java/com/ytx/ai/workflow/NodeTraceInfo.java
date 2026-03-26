package com.ytx.ai.workflow;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 节点执行跟踪信息，用于调试时查看每个节点的详细运行情况。
 * <p>
 * 包含以下信息：
 * <ul>
 *   <li>{@code nodeId}：节点 ID</li>
 *   <li>{@code nodeLabel}：节点显示名称</li>
 *   <li>{@code nodeType}：节点类型（llm/start/end/condition/http/code/subProcess/...）</li>
 *   <li>{@code status}：执行状态（success / skipped / error）</li>
 *   <li>{@code cost}：节点耗时（毫秒）</li>
 *   <li>{@code errorMessage}：错误信息（status=error 时非空）</li>
 *   <li>{@code inputs}：节点输入，key 为字段名，value 为解析后的实际值</li>
 *   <li>{@code outputs}：节点输出，key 为变量名，value 为计算后的实际值</li>
 * </ul>
 */
@Getter
@Setter
@Builder
public class NodeTraceInfo {

    /** 节点 ID */
    private String nodeId;

    /** 节点显示名称 */
    private String nodeLabel;

    /**
     * 节点类型，插件类节点取 componentId（如 "llm"/"start"/"end"），
     * 子流程类节点取 "workflow"
     */
    private String nodeType;

    /**
     * 执行状态：
     * <ul>
     *   <li>success：正常执行完成</li>
     *   <li>skipped：条件不满足被跳过</li>
     *   <li>error：执行过程中抛出异常</li>
     * </ul>
     */
    private String status;

    /** 节点执行耗时（毫秒） */
    private long cost;

    /** 错误信息，status=error 时非空 */
    private String errorMessage;

    /**
     * 节点输入数据，key 为字段名，value 为变量引用解析后的实际值。
     * 数据来源：将节点 NodeMeta（执行前已完成变量解析）序列化为 Map，
     * 不包含 outputs 字段（避免与输出区域重复）。
     */
    private Map<String, Object> inputs;

    /**
     * 节点输出数据，key 为变量名，value 为节点计算后的实际值。
     * 数据来源：优先取 NodeResult.data；若为空则从 NodeMeta.outputs 中提取。
     */
    private Map<String, Object> outputs;
}
