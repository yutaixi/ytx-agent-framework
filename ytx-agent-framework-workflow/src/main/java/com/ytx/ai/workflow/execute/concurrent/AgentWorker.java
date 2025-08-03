package com.ytx.ai.workflow.execute.concurrent;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import com.jd.platform.async.callback.IWorker;
import com.jd.platform.async.wrapper.WorkerWrapper;
import com.ytx.ai.agent.entity.SkillEntity;
import com.ytx.ai.agent.service.SkillService;
import com.ytx.ai.base.agent.AgentTask;
import com.ytx.ai.base.agent.Skill;
import com.ytx.ai.base.agent.SubAgentResponse;
import com.ytx.ai.workflow.Workflow;
import com.ytx.ai.workflow.WorkflowOutput;
import com.ytx.ai.workflow.constant.AgentConstants;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.execute.FlowExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;


@Slf4j
public class AgentWorker implements IWorker<AgentWorkerParam, SubAgentResponse> {


    @Override
    public SubAgentResponse action(AgentWorkerParam workerParam, Map<String, WorkerWrapper> allWrappers) {
        StopWatch stopWatch=new StopWatch();
        stopWatch.start();
        SubAgentResponse response= SubAgentResponse.builder()
                .build();
        Skill skill =workerParam.getSkill();
        if(ObjectUtil.isEmpty(skill)){
            return response;
        }
        AgentTask task=workerParam.getTargetTask();
        response.setAgentName(skill.getName());
        switch (skill.getType()){
            case PLUGIN:
                break;
            case WORKFLOW:
                String workflowId= skill.getId();
                SkillService skillService= SpringUtil.getBean(SkillService.class);
                SkillEntity skillEntity=skillService.findSkill(Integer.valueOf(workflowId));
                Workflow workflow=Workflow.of(skillEntity);
                FlowExecutor flowExecutor= SpringUtil.getBean(FlowExecutor.class);
                WorkflowOutput workflowOutput= flowExecutor.execute(workflow,new FlowContext());
                task.setExecute_result(workflowOutput);
                response.setNeedHumanFeedback(workflowOutput.isStopTheWorld());
                if(ObjectUtil.isNotEmpty(workflowOutput.getAnswer())){
                    response.setAnswer(workflowOutput.getAnswer());
                }else{
                    response.setResult(workflowOutput);
                }
                response.setResult(workflowOutput);
                break;
        }

        stopWatch.stop();
        log.info(AgentConstants.CHAT_TRACE_LOG_MARKER+":{},\n plugin:{} function {},\ntime cost:{}ms,\ntask result:{}",
                workerParam.getChatDTO().getChatId(),
                skill.getName(),
                task.getFunction(),
                stopWatch.getLastTaskTimeMillis(),
                JSONUtil.toJsonStr(response.getResult())
                );
        return response;
    }

    @Override
    public SubAgentResponse defaultValue() {
        return null;
    }
}
