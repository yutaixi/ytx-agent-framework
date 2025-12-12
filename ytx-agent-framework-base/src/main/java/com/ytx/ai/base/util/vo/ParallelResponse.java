package com.ytx.ai.base.util.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 并行处理响应对象
 * 用于封装并行处理所有任务的整体执行结果
 *
 * @author ytx
 */
@Getter
@Setter
@Builder
public class ParallelResponse {
    /**
     * 整体执行是否成功
     * true: 所有任务都执行成功
     * false: 存在失败的任务
     */
    private boolean success;

    /**
     * 成功执行的任务数量
     */
    private int successCount;

    /**
     * 失败执行的任务数量
     */
    private int failedCount;

    /**
     * 失败的任务结果列表
     * 包含所有执行失败的任务详情，用于错误追踪和问题定位
     */
    private List<ParallelTaskResult> failedTasks;
}

