package com.ytx.ai.agent.execute;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.ttl.threadpool.TtlExecutors;
import com.jd.platform.async.executor.Async;
import com.jd.platform.async.wrapper.WorkerWrapper;
import com.ytx.ai.agent.SkillRegister;
import com.ytx.ai.agent.config.TaskExecutorProperty;
import com.ytx.ai.agent.constant.AgentConstants;
import com.ytx.ai.agent.constant.MemoryConstants;
import com.ytx.ai.agent.dto.ChatDTO;
import com.ytx.ai.agent.execute.concurrent.AgentWorker;
import com.ytx.ai.agent.execute.concurrent.AgentWorkerCallback;
import com.ytx.ai.agent.execute.concurrent.AgentWorkerParam;
import com.ytx.ai.agent.service.AgentMemoryService;
import com.ytx.ai.agent.skill.AiAgent;
import com.ytx.ai.agent.skill.ReplyToUserAgent;
import com.ytx.ai.agent.tool.TaskTools;
import com.ytx.ai.agent.util.StringUtils;
import com.ytx.ai.agent.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class AgentTaskExecutor {

    @Autowired
    private SkillRegister skillRegister;

    @Autowired
    TaskExecutorProperty taskExecutorProperty;

    @Autowired
    AgentMemoryService agentMemoryService;

    private static final ThreadPoolExecutor COMMON_POOL=(ThreadPoolExecutor) Executors.newCachedThreadPool();

    private static final ExecutorService ttlExecutorService= TtlExecutors.getTtlExecutorService(COMMON_POOL);

    public AgentResponse run(ChatDTO chatDTO, PlannedTasks plannedTasks){
        List<Map.Entry<Integer, List<AgentTask>>> reorderedTasks= TaskTools.reorderTasks(plannedTasks.getTasks());
        String finalReply=null;
        AgentExecuteContext agentExecuteContext=new AgentExecuteContext();
        agentExecuteContext.setMemory(getMemory(chatDTO));
        boolean isNeedHumanFeedBack=false;

        for(Map.Entry<Integer,List<AgentTask>> entry :reorderedTasks){
            List<AgentTask> parallelTasks=entry.getValue();
            List<WorkerWrapper> workers=new ArrayList<>();
            AgentWorkerCallback callback=new AgentWorkerCallback();
            for(AgentTask task : parallelTasks){
                AiAgent agent=skillRegister.getAgent(task.getAgent());
                if(ObjectUtil.isNull(agent))
                {
                    continue;
                }
                AgentTask targetTask=preprocessTask(agent,task,plannedTasks);
                AgentWorkerParam workerParam=AgentWorkerParam.builder().agent(agent)
                        .targetTask(targetTask)
                        .chatDTO(chatDTO)
                        .agentExecuteContext(agentExecuteContext)
                        .build();
                AgentWorker worker=new AgentWorker();
                WorkerWrapper<AgentWorkerParam,SubAgentResponse> workerWrapper=new WorkerWrapper.Builder<AgentWorkerParam,SubAgentResponse>()
                        .worker(worker)
                        .param(workerParam)
                        .callback(callback)
                        .build();
                workers.add(workerWrapper);
            }

            try{
                StopWatch stopWatch=new StopWatch();
                stopWatch.start();
                Async.beginWork(taskExecutorProperty.getAsyncWorkerTimeout(),ttlExecutorService,workers);
                afterEachGroupExecuted(callback,agentExecuteContext);
                stopWatch.stop();
                log.info(AgentConstants.CHAT_TRACE_LOG_MARKER+":{},\ntask group {} time cost:{}ms",
                        chatDTO.getChatId(),
                        entry.getKey(),
                        stopWatch.getLastTaskTimeMillis()
                        );
            }catch(ExecutionException | InterruptedException e){
                throw new RuntimeException(e);
            }
            if(callback.isNeedHumanFeedback())
            {
                AgentTask task=formatReplyAgentTask(plannedTasks,null);
                SubAgentResponse result=skillRegister.getAgent(ReplyToUserAgent.NAME).execute(chatDTO,task,agentExecuteContext);
                finalReply=String.valueOf(result.getResult());
                isNeedHumanFeedBack=true;
                break;
            }
        }
        if(!isNeedHumanFeedBack){
            SubAgentResponse replyAgentResponse=agentExecuteContext.get(ReplyToUserAgent.NAME);
            finalReply=String.valueOf(replyAgentResponse.getResult()) ;
        }
        AgentResponse agentResponse=AgentResponse.builder()
                .result(finalReply)
                .commands(agentExecuteContext.getCommands())
                .build();
        afterAllTaskExecuted(chatDTO,agentExecuteContext);
        return agentResponse;

    }

    private void afterEachGroupExecuted(AgentWorkerCallback callback,AgentExecuteContext context){
        if(ObjectUtil.isNotEmpty(callback.getCommands())){
            context.getCommands().addAll(callback.getCommands());
        }

        if(ObjectUtil.isNotEmpty(callback.getMemoryTraces())){
            context.getMemory().putAll(callback.getMemoryTraces());
        }
    }

    private void afterAllTaskExecuted(ChatDTO chatDTO,AgentExecuteContext context){

        AgentMemory agentMemory=context.getMemory();
        agentMemoryService.saveMemory(chatDTO.getChatId(),agentMemory);
    }

    private AgentMemory getMemory(ChatDTO chatDTO)
    {
        AgentMemory agentMemory=agentMemoryService.getMemory(chatDTO.getChatId());
        if(ObjectUtil.isNull(agentMemory))
        {
            agentMemory=new AgentMemory();
        }
        Map<String,String> propertyMap=chatDTO.getProperties();
        if(ObjectUtil.isEmpty(propertyMap)){
            return agentMemory;
        }
        agentMemory.put(MemoryConstants.USER_PREFERRED_LANGUAGE,propertyMap.get("language"));
        agentMemory.put(MemoryConstants.CURRENT_LOCATION,propertyMap.get("country"));
        return agentMemory;
    }

    private AgentTask preprocessTask(AiAgent agent,AgentTask task,PlannedTasks plannedTasks){
        AgentTask targetTask=task;
        if(ReplyToUserAgent.NAME.equalsIgnoreCase(agent.getName())){
            targetTask=formatReplyAgentTask(plannedTasks,task);
        }
        return targetTask;
    }

    private AgentTask formatReplyAgentTask(PlannedTasks plannedTasks,AgentTask originalTask){
        plannedTasks.getTasks().stream()
                .filter(task->!ReplyToUserAgent.NAME.equalsIgnoreCase(task.getAgent()))
                .forEach(task->{
                    if(ObjectUtil.isEmpty(task.getExecute_result())  || StringUtils.isBlank(task.getExecute_result().toString())){
                        task.setExecute_result(taskExecutorProperty.getTaskNotExecuteResult());
                    }
                });
        AgentTask replyAgentTask=new AgentTask();
        replyAgentTask.setAgent(ReplyToUserAgent.NAME);
        replyAgentTask.setParams(plannedTasks);
        if(ObjectUtil.isNotEmpty(originalTask)){
            replyAgentTask.setTask_id(originalTask.getTask_id());
            replyAgentTask.setObjective(replyAgentTask.getObjective());
            replyAgentTask.setExecute_order(replyAgentTask.getExecute_order());

        }
        return replyAgentTask;
    }
}
