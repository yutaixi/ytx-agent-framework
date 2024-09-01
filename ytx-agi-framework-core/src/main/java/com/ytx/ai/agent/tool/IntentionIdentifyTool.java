package com.ytx.ai.agent.tool;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
import com.plexpt.chatgpt.entity.chat.Message;
import com.plexpt.chatgpt.entity.chat.ResponseFormat;
import com.ytx.ai.agent.AgentHolder;
import com.ytx.ai.agent.Intention.Intention;
import com.ytx.ai.agent.Intention.OtherIntent;
import com.ytx.ai.agent.Intention.SpecialIntentProcessor;
import com.ytx.ai.agent.config.IntentAgentProperty;
import com.ytx.ai.agent.constant.AgentConstants;
import com.ytx.ai.agent.dto.ChatDTO;
import com.ytx.ai.agent.execute.AgentExecuteContext;
import com.ytx.ai.agent.util.StringUtils;
import com.ytx.ai.agent.vo.AgentTask;
import com.ytx.ai.agent.vo.SubAgentResponse;
import com.ytx.ai.agent.vo.UserIntention;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static com.ytx.ai.agent.constant.AgentConstants.JSON_OBJECT;

@Slf4j
public class IntentionIdentifyTool extends BaseTool{

    public static final String SYS_PROMPT="intent_agent_sys_prompt";

    public static final String INTENT_UNCLEAR_REPLY="intent_agent_intent_unclear_reply";

    public static final String NAME="intention_identify";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Intention identify tool";
    }

    @Autowired
    AgentHolder agentHolder;

    @Autowired
    IntentAgentProperty intentAgentProperty;

    @Autowired(required = false)
    SpecialIntentProcessor specialIntentProcessor;

    @Override
    public SubAgentResponse execute(ChatDTO chatDTO, AgentTask task, AgentExecuteContext context) {
        super.execute(chatDTO, task, context);
        UserIntention intention=specialCase(chatDTO.getQuestion());
        if(ObjectUtil.isNotNull(intention)){
            return SubAgentResponse.builder()
                    .agentName(getName())
                    .result(intention)
                    .build();
        }
        List<Message> messages=new ArrayList<>();
        messages.add(Message.ofSystem(getSysPrompt()));
        if(ObjectUtil.isNotEmpty(chatDTO.getHistory())){
            messages.addAll(chatDTO.getHistory());
        }
        messages.add(Message.of(chatDTO.getQuestion()));
        ChatCompletion chatCompletion=ChatCompletion.builder()
                .model(ChatCompletion.Model.GPT4o)
                .messages(messages)
                .temperature(intentAgentProperty.getChatTemperature())
                .responseFormat(ResponseFormat.builder().type(JSON_OBJECT).build())
                .build();
        ChatCompletionResponse response=llmService.chatCompletion(chatCompletion);
        if(ObjectUtil.isNull(response))
        {
            log.error(AgentConstants.CHAT_TRACE_LOG_MARKER+":{},{} run chatCompletion failed.",chatDTO.getChatId(),getName());
        }
        String jsonData=String.valueOf(response.getChoices().get(0).getMessage().getContent());
        UserIntention userIntention= JSONUtil.toBean(jsonData,UserIntention.class);
        return SubAgentResponse.builder()
                .agentName(getName())
                .result(verifyIntention(userIntention))
                .build();

    }

    private UserIntention verifyIntention(UserIntention userIntention){
        if(StringUtils.isBlank(userIntention.getIntent())
        && StringUtils.isBlank(userIntention.getTag())
                && StringUtils.isBlank(userIntention.getReply())
                && (StringUtils.isBlank(userIntention.getIsClear()) || !isUserIntentionClear(userIntention))
        ){
            userIntention.setIsClear("false");
            userIntention.setIntent("Unknown");
            userIntention.setReply(agentHolder.getProperty(INTENT_UNCLEAR_REPLY));
        }else if(isUserIntentionClear(userIntention) && StringUtils.isBlank(userIntention.getTag())){
            userIntention.setTag(OtherIntent.NAME);
        }
        return userIntention;
    }

    private UserIntention specialCase(String question){
        if(ObjectUtil.isNull(specialIntentProcessor)){
            return null;
        }
        return specialIntentProcessor.process(question);
    }
    public static boolean isUserIntentionClear(UserIntention userIntention){
        if (ObjectUtil.isEmpty(userIntention)
                || "false".equalsIgnoreCase(userIntention.getIsClear())
                || "no".equalsIgnoreCase(userIntention.getIsClear())) {
            return false;
        }
        return true;
    }

    private String getSysPrompt(){
        String intentionTags="";
        List<Intention> intentions=agentHolder.getRefIntentions();
        if(ObjectUtil.isNotEmpty(intentions)){
            StringBuilder sb=new StringBuilder();
            intentions.stream().filter(Intention::intentRecognition).forEach(intention -> {
                if(!intention.intentRecognition()){
                    return;
                }
                sb.append("- ").append(intention.getName()).append(":").append(intention.getDesc()).append("\n");
            });
            intentionTags=sb.toString();
        }
        return agentHolder.getProperty(SYS_PROMPT).replace("{{intentionTags}}",intentionTags);
    }
}
