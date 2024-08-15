package com.ytx.ai.agent.execute.concurrent;

import cn.hutool.core.date.StopWatch;
import cn.hutool.json.JSONUtil;
import com.jd.platform.async.callback.IWorker;
import com.jd.platform.async.wrapper.WorkerWrapper;
import com.ytx.ai.agent.vo.SubAgentResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.ytx.ai.agent.constant.AgentConstants.CHAT_TRACE_LOG_MARKER;

@Slf4j
public class AgentWorker implements IWorker<AgentWorkerParam, SubAgentResponse> {
    @Override
    public SubAgentResponse action(AgentWorkerParam workerParam, Map<String, WorkerWrapper> allWrappers) {
        StopWatch stopWatch=new StopWatch();
        stopWatch.start();
        SubAgentResponse result=workerParam.getAgent().execute(
                workerParam.getChatDTO(),
                workerParam.getTargetTask(),
                workerParam.getAgentExecuteContext()
        );
        workerParam.getTargetTask().setExecute_result(result.getResult());
        stopWatch.stop();
        log.info(CHAT_TRACE_LOG_MARKER+":{},\ntask agent:{},\ntime cost:{}ms,\ntask result:{}",
                workerParam.getChatDTO().getChatId(),
                workerParam.getAgent().getName(),
                stopWatch.getLastTaskTimeMillis(),
                JSONUtil.toJsonStr(result));
        return null;
    }

    @Override
    public SubAgentResponse defaultValue() {
        return null;
    }
}
