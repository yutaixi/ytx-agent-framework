package com.ytx.ai.base.util.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 并行任务结果对象
 * 用于封装单个并行任务的执行结果
 *
 * @author ytx
 */
@Getter
@Setter
@Builder
public class ParallelTaskResult {
    /**
     * 任务处理的数据对象
     * 可以是任何类型的数据，例如文件路径、数据对象等
     */
    private Object data;

    /**
     * 任务执行是否成功
     * true: 执行成功
     * false: 执行失败
     */
    private boolean success;

    /**
     * 任务执行结果消息
     * 成功时可以为空，失败时包含错误信息
     */
    private String message;
}

