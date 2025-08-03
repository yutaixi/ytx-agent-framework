package com.ytx.ai.workflow.execute.concurrent;

import cn.hutool.core.util.ObjectUtil;
import com.jd.platform.async.callback.ICallback;
import com.jd.platform.async.worker.WorkResult;
import com.ytx.ai.base.agent.Command;
import com.ytx.ai.base.agent.MemoryTrace;
import com.ytx.ai.base.agent.SubAgentResponse;
import com.ytx.ai.workflow.constant.AgentConstants;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


@Slf4j
@Getter
public class AgentWorkerCallback implements ICallback<AgentWorkerParam, SubAgentResponse> {

    boolean needHumanFeedback;
    final List<Command> commands=new CopyOnWriteArrayList<>();
    final List<MemoryTrace> memoryTraces=new CopyOnWriteArrayList<>();


    @Override
    public void begin() {

    }

    @Override
    public void result(boolean success, AgentWorkerParam param, WorkResult<SubAgentResponse> workResult) {

        if(!success){
            String jobState="";
            String exception="";
            if(ObjectUtil.isNotNull(workResult.getResultState())){
                jobState=workResult.getResultState().name();
            }
            if(ObjectUtil.isNotNull(workResult.getEx())){
                exception=workResult.getEx().toString();
            }
            log.error(AgentConstants.CHAT_TRACE_LOG_MARKER+":{},\nplugin {} function {} worker failed,\nresultState:{},\nexception:{}",
                    param.getChatDTO().getChatId(),
                    param.getSkill().getName(),
                    param.getTargetTask().getFunction(),
                    jobState,
                    exception);
            return;
        }
        if(workResult.getResult().isNeedHumanFeedback() && !needHumanFeedback){
            needHumanFeedback=true;
        }
        if(ObjectUtil.isNotEmpty(workResult.getResult().getCommands())){
            commands.addAll(workResult.getResult().getCommands());
        }
        if(ObjectUtil.isNotEmpty(workResult.getResult().getMemoryTraces())){
            memoryTraces.addAll(workResult.getResult().getMemoryTraces());
        }
    }
}
