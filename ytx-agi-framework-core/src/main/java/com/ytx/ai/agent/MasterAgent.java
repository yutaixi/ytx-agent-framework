package com.ytx.ai.agent;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.ytx.ai.agent.constant.AgentConstants;
import com.ytx.ai.agent.dto.ChatDTO;
import com.ytx.ai.agent.execute.AgentTaskExecutor;
import com.ytx.ai.agent.skill.ReplyToUserAgent;
import com.ytx.ai.agent.tool.IntentionIdentifyTool;
import com.ytx.ai.agent.vo.AgentResponse;
import com.ytx.ai.agent.vo.AgentTask;
import com.ytx.ai.agent.vo.PlannedTasks;
import com.ytx.ai.agent.vo.UserIntention;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class MasterAgent {

    @Autowired
    PlanAgent planAgent;

    @Autowired
    IntentionAgent intentionAgent;

    @Autowired
    AgentTaskExecutor agentTaskExecutor;

    public AgentResponse run(ChatDTO chatDTO) {

        StopWatch stopWatch = new StopWatch();
        AgentResponse taskExecutorResponse = null;
        PlannedTasks plannedTasks = null;
        log.info(AgentConstants.CHAT_TRACE_LOG_MARKER + ":{},\nuser question:{},\nproperties:{}.", chatDTO.getChatId(), chatDTO.getQuestion(), chatDTO.getProperties());
        stopWatch.start("Intent Recognize");
        UserIntention userIntention = intentionAgent.run(chatDTO);
        stopWatch.stop();
        log.info(AgentConstants.CHAT_TRACE_LOG_MARKER + ":{},\nuser intention:{},\ntime cost:{}ms.", chatDTO.getChatId(), JSONUtil.toJsonStr(userIntention), stopWatch.getLastTaskTimeMillis());
        if (!IntentionIdentifyTool.isUserIntentionClear(userIntention)) {
            taskExecutorResponse = askUserToClearIntention(chatDTO, userIntention);
        } else {
            stopWatch.start("Task Planning");
            plannedTasks = planAgent.run(chatDTO, userIntention);
            stopWatch.stop();
            log.info(AgentConstants.CHAT_TRACE_LOG_MARKER + ":{},\nplanned tasks:{},\ntime cost:{}ms.", chatDTO.getChatId(), JSONUtil.toJsonStr(plannedTasks), stopWatch.getLastTaskTimeMillis());
            stopWatch.start("Task Execute");
            taskExecutorResponse = agentTaskExecutor.run(chatDTO, plannedTasks);
            stopWatch.stop();
            log.info(AgentConstants.CHAT_TRACE_LOG_MARKER + ":{},\ntask executor time cost:{}ms.", chatDTO.getChatId(), stopWatch.getLastTaskTimeMillis());
        }

        log.info(AgentConstants.CHAT_TRACE_LOG_MARKER + ":{},\n" +
                        "user input:{}\n" +
                        "user intent:{}\n" +
                        "planned tasks:{}\n" +
                        "final response:{}\n" +
                        "total cost:{}ms",
                chatDTO.getChatId(),
                chatDTO.getQuestion(),
                JSONUtil.toJsonStr(userIntention),
                ObjectUtil.isNotNull(plannedTasks) ? plannedTasks.getTaskNames() : "",
                JSONUtil.toJsonStr(taskExecutorResponse),
                stopWatch.getTotalTimeMillis()
        );
        return taskExecutorResponse;

    }


    private AgentResponse askUserToClearIntention(ChatDTO chatDTO, UserIntention userIntention){
        PlannedTasks plannedTasks=new PlannedTasks();
        AgentTask agentTask=new AgentTask();
        agentTask.setTask_id("1");
        agentTask.setAgent(ReplyToUserAgent.NAME);
        agentTask.setObjective("Ask the user what they would like to do in a polite manner,for example:"+userIntention.getReply());
        agentTask.setExecute_order(1);

        plannedTasks.setTasks(ListUtil.toList(agentTask));
        return agentTaskExecutor.run(chatDTO,plannedTasks);
    }
}
