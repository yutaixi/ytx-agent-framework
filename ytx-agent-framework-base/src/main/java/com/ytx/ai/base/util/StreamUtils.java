package com.ytx.ai.base.util;

import com.google.common.collect.Streams;
import com.ytx.ai.base.util.vo.ParallelResponse;
import com.ytx.ai.base.util.vo.ParallelTaskResult;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * 流处理工具类
 * 提供并行处理集合和流数据的能力，支持并发控制、异常处理和任务中断
 *
 * @author ytx
 */
@Slf4j
public class StreamUtils {

    /**
     * 默认并发数
     * 控制同时执行的任务数量，避免资源过度消耗
     */
    private static final int DEFAULT_PARALLELISM = 5;

    /**
     * 默认等待终止时间（秒）
     * 线程池关闭时等待任务完成的最大时间
     * 预留字段，当前未使用，可用于未来扩展
     */
    @SuppressWarnings("unused")
    private static final int DEFAULT_AWAIT_TERMINATION_SECONDS = 3;

    /**
     * 终止标志
     * 用于在任务执行过程中提前终止新任务的启动
     * 当检测到严重错误（如OOM）时，设置此标志可以快速停止后续任务
     */
    private static final AtomicBoolean STOP_FLAG = new AtomicBoolean(false);

    /**
     * 并行处理集合数据
     * 使用默认并发数处理集合中的每个元素
     *
     * @param dataList 待处理的数据集合
     * @param consumer 处理每个数据的消费者函数
     * @param <T>      数据类型
     * @return 并行处理结果，包含成功数量、失败数量和失败任务列表
     */
    public static <T> ParallelResponse parallel(Collection<T> dataList, Consumer<T> consumer) {
        return parallel(dataList.stream(), consumer, DEFAULT_PARALLELISM);
    }

    /**
     * 并行处理集合数据
     * 使用指定的并发数处理集合中的每个元素
     *
     * @param dataList   待处理的数据集合
     * @param consumer   处理每个数据的消费者函数
     * @param parallelism 并发数，控制同时执行的任务数量
     * @param <T>        数据类型
     * @return 并行处理结果，包含成功数量、失败数量和失败任务列表
     */
    public static <T> ParallelResponse parallel(Collection<T> dataList, Consumer<T> consumer, int parallelism) {
        return parallel(dataList.stream(), consumer, parallelism);
    }

    /**
     * 并行处理流数据
     * 使用默认并发数处理流中的每个元素
     *
     * @param stream   待处理的数据流
     * @param consumer 处理每个数据的消费者函数
     * @param <T>      数据类型
     * @return 并行处理结果，包含成功数量、失败数量和失败任务列表
     */
    public static <T> ParallelResponse parallel(Stream<T> stream, Consumer<T> consumer) {
        return parallel(stream, consumer, DEFAULT_PARALLELISM);
    }

    /**
     * 并行处理流数据
     * 使用指定的并发数处理流中的每个元素，支持任务中断和异常处理
     *
     * @param stream     待处理的数据流
     * @param consumer   处理每个数据的消费者函数
     * @param parallelism 并发数，控制同时执行的任务数量
     * @param <T>        数据类型
     * @return 并行处理结果，包含成功数量、失败数量和失败任务列表
     */
    public static <T> ParallelResponse parallel(Stream<T> stream, Consumer<T> consumer, int parallelism) {
        // 每次运行前重置终止标志
        STOP_FLAG.set(false);

        // 存储所有异步任务的Future对象
        List<CompletableFuture<ParallelTaskResult>> futureList = new ArrayList<>();

        try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor()) {
            // 限制并发数
            final Semaphore POOL = new Semaphore(parallelism);

            // 使用Streams.mapWithIndex为每个数据元素创建异步任务
            Streams.mapWithIndex(stream, (data, index) -> {
                // 创建异步任务
                CompletableFuture<ParallelTaskResult> future = CompletableFuture.supplyAsync(() -> {
                    // 初始化任务结果对象
                    ParallelTaskResult result = ParallelTaskResult.builder().data(data).build();

                    try {
                        // 如果已经被要求停止,提前结束
                        if (STOP_FLAG.get()) {
                            result.setSuccess(false);
                            result.setMessage("Task stopped before start.");
                            return result;
                        }

                        // 获取信号量(注:信号量用完了,后面的任务就只能等着)
                        POOL.acquire();

                        // 再次检查停止标志和线程中断状态
                        if (STOP_FLAG.get() || Thread.currentThread().isInterrupted()) {
                            result.setSuccess(false);
                            result.setMessage("Task interrupted before execution.");
                            return result;
                        }

                        // 保护性捕获所有异常/错误,记录触发异常的 data(例如文件路径)
                        try {
                            // 执行消费者函数处理数据
                            consumer.accept(data);
                            result.setSuccess(true);
                        } catch (Throwable t) {
                            // 打印出导致错误的文件/数据标识
                            String dataId = (data == null) ? "null" : data.toString();
                            log.error("Task error for data: {}", dataId, t);
                            result.setSuccess(false);
                            result.setMessage("Error:" + t.getClass().getName() + ":" + t.getMessage());

                            // 如果是OOM,设置停止标志以尽快阻止新任务启动
                            if (t instanceof OutOfMemoryError) {
                                log.error("OutOfMemoryError detected, setting stop flag.");
                                STOP_FLAG.set(true);
                            }
                        } finally {
                            // 确保在发生 Throwable 时也能释放信号量
                        }
                    } catch (InterruptedException e) {
                        // 保持中断状态
                        Thread.currentThread().interrupt();
                        log.warn("Task interrupted: {}", data);
                        result.setSuccess(false);
                        result.setMessage("Interrupted:" + e.getMessage());
                    } finally {
                        // 执行完后,释放信号量
                        POOL.release();
                    }

                    return result;
                }, service);

                // 将Future添加到列表中
                futureList.add(future);
                return null;
            }).toList();

            // 等待所有任务完成
            CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("parallel run error.", e);
            throw new RuntimeException(e);
        }

        // 格式化并返回结果
        return formatResult(futureList);
    }

    /**
     * 格式化并行处理结果
     * 统计成功和失败的任务数量，并收集失败的任务详情
     *
     * @param futures 所有任务的Future列表
     * @return 格式化后的并行处理响应对象
     */
    private static ParallelResponse formatResult(List<CompletableFuture<ParallelTaskResult>> futures) {
        // 初始化响应对象
        ParallelResponse response = ParallelResponse.builder().build();
        List<ParallelTaskResult> failedResultList = new ArrayList<>();

        // 遍历所有Future，获取任务执行结果
        futures.forEach(future -> {
            try {
                // 获取任务结果（阻塞等待）
                ParallelTaskResult taskResult = future.get();

                // 根据任务执行结果统计成功和失败数量
                if (taskResult.isSuccess()) {
                    response.setSuccessCount(response.getSuccessCount() + 1);
                } else {
                    response.setFailedCount(response.getFailedCount() + 1);
                    failedResultList.add(taskResult);
                }
            } catch (Exception e) {
                // 处理获取结果时的异常
                log.error("处理结果异常", e);
                response.setFailedCount(response.getFailedCount() + 1);
            }
        });

        // 设置失败任务列表
        response.setFailedTasks(failedResultList);

        // 如果所有任务都成功，设置整体成功标志
        if (response.getSuccessCount() == futures.size()) {
            response.setSuccess(true);
        }

        return response;
    }

    /**
     * 外部可调用的停止方法
     * 设置停止标志，用于提前终止并行任务的执行
     * 调用此方法后，正在等待执行的任务将不会启动，已启动的任务会继续执行完成
     */
    public static void stop() {
        STOP_FLAG.set(true);
    }
}

