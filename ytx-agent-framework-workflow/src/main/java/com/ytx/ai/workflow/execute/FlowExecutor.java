package com.ytx.ai.workflow.execute;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.alibaba.ttl.threadpool.TtlExecutors;
import com.jd.platform.async.executor.Async;
import com.jd.platform.async.worker.DependWrapper;
import com.jd.platform.async.wrapper.WorkerWrapper;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeOutput;
import com.ytx.ai.workflow.Workflow;
import com.ytx.ai.workflow.WorkflowOutput;
import com.ytx.ai.workflow.execute.concurrent.FlowWorker;
import com.ytx.ai.workflow.execute.concurrent.FlowWorkerParam;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Slf4j
public class FlowExecutor {

    // asyncTool默认的线程池
    private static final ThreadPoolExecutor COMMON_POOL = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    private static final ExecutorService ttlExecutorService = TtlExecutors.getTtlExecutorService(COMMON_POOL);

    public WorkflowOutput execute(Workflow workFlow, FlowContext context) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        WorkflowOutput workFlowOutput = WorkflowOutput.of();
        if (ObjectUtil.isEmpty(workFlow.getNodes())) {
            return workFlowOutput;
        }

        // 处理context，入参为空则新建
        FlowContext finalContext;
        if (context == null) {
            finalContext = FlowContext.of();
        } else {
            finalContext = context;
        }

        // 新流程包装类，方便后续使用
        WorkflowWrapper workFlowWrapper = new WorkflowWrapper(workFlow);
        finalContext.setWorkflowWrapper(workFlowWrapper);

        // worker Map
        Map<String, WorkerWrapper> workerWrapperMap = new HashMap<>();

        // 先创建所有节点任务，无依赖关系
        workFlow.getNodes().forEach(node -> {
            // worker参数
            FlowWorkerParam flowWorkerParam = FlowWorkerParam.builder()
                    .flowNode(node)
                    .flowContext(finalContext)
                    .workflowOutput(workFlowOutput)
                    .build();

            // 工作处理类
            FlowWorker flowWorker = new FlowWorker();
            WorkerWrapper<FlowWorkerParam, NodeOutput> workerWrapper = new WorkerWrapper.Builder<FlowWorkerParam, NodeOutput>()
                    .worker(flowWorker)
                    .param(flowWorkerParam)
                    .callback(flowWorker)
                    .build();

            workerWrapperMap.put(node.getId(), workerWrapper);
        });

        // 增加节点依赖关系
        workerWrapperMap.forEach((id, wrapper) -> {
            List<FlowNode> childNodes = workFlowWrapper.getChildNodes(id);
            if (ObjectUtil.isNotEmpty(childNodes)) {
                ReflectUtil.invoke(wrapper,
                         "addNextWrappers",
                        getWorkerWrapper(childNodes, workerWrapperMap));
            }

            List<FlowNode> parentNodes = workFlowWrapper.getParentNodes(id);
            if (ObjectUtil.isNotEmpty(parentNodes)) {
                ReflectUtil.invoke(wrapper,
                         "addDependWrappers",
                        getDependWrapper(parentNodes, workerWrapperMap));
                getWorkerWrapper(parentNodes, workerWrapperMap).forEach(workerWrapper -> {
                    ReflectUtil.invoke(wrapper, "addDepend", workerWrapper, true);
                });
            }
        });

        // 所有worker集合
        List<WorkerWrapper> workers = workerWrapperMap.values().stream()
                .filter(item -> ObjectUtil.isEmpty(item.getDependWrappers()))
                .toList();

        try {
            Async.beginWork(30000L, ttlExecutorService, workers);
        } catch (ExecutionException | InterruptedException e) {
            stopWatch.stop();
            throw new RuntimeException(e);
        }

        stopWatch.stop();
        log.info("workflow " + workFlow.getName() + ":" + workFlow.getId() +
                " time cost: " + stopWatch.getLastTaskTimeMillis() + "ms");


        summary(workFlowOutput, finalContext, workFlowWrapper, stopWatch.getLastTaskTimeMillis());
        return workFlowOutput;
    }


    private void summary(WorkflowOutput workflowOutput, FlowContext context, WorkflowWrapper workflowWrapper, long costTotal) {
        Workflow workflow = workflowWrapper.getWorkflow();
        Map<String, NodeOutput> outputMap = context.getNodeOutputMap();
        StringBuilder costSummary = new StringBuilder(workflow.getName() + " cost summary:");

        if (ObjectUtil.isNotEmpty(outputMap)) {
            outputMap.forEach((id, output) -> {
                FlowNode node = workflowWrapper.getNode(id);
                costSummary.append("\nnode id:")
                        .append(id).append(", node name:")
                        .append(node.getLabel())
                        .append(", cost:")
                        .append(output.getCost())
                        .append("ms.");
            });
        }

        costSummary.append("\nTotal cost:").append(costTotal).append("ms.");
        workflowOutput.setCostSummary(costSummary.toString());
    }


    private List<DependWrapper> getDependWrapper(List<FlowNode> nodes, Map<String, WorkerWrapper> workerWrapperMap) {
        List<WorkerWrapper> workerWrappers = getWorkerWrapper(nodes, workerWrapperMap);
        return workerWrappers.stream().map(item -> {
            return new DependWrapper(item, true);
        }).collect(Collectors.toList());
    }

    private List<WorkerWrapper> getWorkerWrapper(List<FlowNode> nodes, Map<String, WorkerWrapper> workerWrapperMap) {
        if (ObjectUtil.isEmpty(nodes)) {
            return null;
        }

        List<WorkerWrapper> workerWrappers = new ArrayList<>();
        nodes.forEach(node -> {
            WorkerWrapper workerWrapper = workerWrapperMap.get(node.getId());
            if (ObjectUtil.isNotEmpty(workerWrapper)) {
                workerWrappers.add(workerWrapper);
            }
        });
        return workerWrappers;
    }
}
