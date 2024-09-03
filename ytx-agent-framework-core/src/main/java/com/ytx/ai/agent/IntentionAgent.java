package com.ytx.ai.agent;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.ytx.ai.agent.config.AgentProperty;
import com.ytx.ai.agent.dto.ChatDTO;
import com.ytx.ai.agent.execute.AgentExecuteContext;
import com.ytx.ai.agent.execute.AgentTaskExecutor;
import com.ytx.ai.agent.tool.CrisisIdentifyTool;
import com.ytx.ai.agent.tool.IntentionIdentifyTool;
import com.ytx.ai.agent.vo.CrisisIdentification;
import com.ytx.ai.agent.vo.PlannedTasks;
import com.ytx.ai.agent.vo.SubAgentResponse;
import com.ytx.ai.agent.vo.UserIntention;
import com.ytx.ai.llm.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class IntentionAgent {

    @Autowired
    LlmService llmService;

    @Autowired
    AgentHolder agentHolder;

    @Autowired
    IntentionIdentifyTool intentionIdentifyTool;

    @Autowired
    AgentTaskExecutor agentTaskExecutor;

    @Autowired
    AgentProperty agentProperty;

    private String toolTasks="{\n" +
            "  \"tasks\":[\n" +
            "    {\n" +
            "  \"task_id\":\"1\",\n" +
            "  \"objective\":\"crisis identify.\",\n" +
            "  \"agent\":\"crisys_identify\",\n" +
            "  \"execute_order\":1,\n" +
            "  \"context\":{}\n" +
            "}\n" +
            "  ]\n" +
            "}";

    public UserIntention run(ChatDTO chatDTO){
        AgentExecuteContext context=new AgentExecuteContext();
        if(agentProperty.isEnableChatToAgent()){
            PlannedTasks toolTasks= JSONUtil.toBean(this.toolTasks,PlannedTasks.class);
            agentTaskExecutor.runAsync(chatDTO,toolTasks,context);
        }
        SubAgentResponse intentResponse=intentionIdentifyTool.execute(chatDTO,null,context);
        context.put(IntentionIdentifyTool.NAME,intentResponse);


        if(agentProperty.isEnableChatToAgent()){
            int loop=0;
            long start=System.currentTimeMillis();
            while(!isTimeout(start) && loop<10){
                if(ObjectUtil.isNull(context.get(CrisisIdentifyTool.NAME))){
                    try{
                        Thread.sleep(500L);
                    }catch (InterruptedException e){
                        log.error("sleep error",e);
                    }
                }else
                {
                    break;
                }
                loop++;
            }
        }
        return getIntentionFromContext(context);
    }

    private boolean isTimeout(long startTime){
        return System.currentTimeMillis()-startTime>5000L;
    }

    private UserIntention getIntentionFromContext(AgentExecuteContext context){
        SubAgentResponse crisisResponse=context.get(CrisisIdentifyTool.NAME);
        if(ObjectUtil.isNotNull(crisisResponse))
        {
            CrisisIdentification crisis=crisisResponse.getBean(CrisisIdentification.class);
            if(ObjectUtil.isNotNull(crisis) && crisis.isMatch()){
                UserIntention intention=UserIntention.builder()
                        .intent("crisis situation need to chat with human agent.")
                        .tag("crisis situation")
                        .reply(crisis.getReply())
                        .isClear("true")
                        .reason(crisis.getReason())
                        .build();
                return intention;
            }
        }
        SubAgentResponse intentionResponse=context.get(IntentionIdentifyTool.NAME);
        return intentionResponse.getBean(UserIntention.class);
    }

}
