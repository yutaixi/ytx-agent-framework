package com.ytx.ai.agent.tool;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
import com.plexpt.chatgpt.entity.chat.Message;
import com.plexpt.chatgpt.entity.chat.ResponseFormat;
import com.ytx.ai.agent.AgentHolder;
import com.ytx.ai.agent.config.CrisisIdentityProperty;
import com.ytx.ai.agent.constant.AgentConstants;
import com.ytx.ai.agent.dto.ChatDTO;
import com.ytx.ai.agent.execute.AgentExecuteContext;
import com.ytx.ai.agent.util.StringUtils;
import com.ytx.ai.agent.vo.AgentTask;
import com.ytx.ai.agent.vo.CrisisIdentification;
import com.ytx.ai.agent.vo.SubAgentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static com.ytx.ai.agent.constant.AgentConstants.JSON_OBJECT;


@Slf4j
public class CrisisIdentifyTool extends BaseTool {

    public static final String NAME="crisis_identify";

    public static final String SYS_PROMPT="crisis_identify_sys_prompt";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "危机事件识别，立即转给人工处理，避免扩大负面影响";
    }

    @Autowired
    AgentHolder agentHolder;

    @Autowired
    CrisisIdentityProperty crisisIdentityProperty;

    @Override
    public SubAgentResponse execute(ChatDTO chatDTO, AgentTask task, AgentExecuteContext context) {
        super.execute(chatDTO, task, context);
        if(!crisisIdentityProperty.isEnable()){
            return SubAgentResponse.builder()
                    .agentName(getName())
                    .result(CrisisIdentification.builder().reason("crisis identify not enabled."))
                    .build();
        }
        if(!preCheck()){

            return SubAgentResponse.builder()
                    .agentName(getName())
                    .result(CrisisIdentification.builder().reason("sys prompt is blank").build())
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
                .temperature(crisisIdentityProperty.getChatTemperature())
                .responseFormat(ResponseFormat.builder().type(JSON_OBJECT).build())
                .build();
        ChatCompletionResponse response=llmService.chatCompletion(chatCompletion);
        if(ObjectUtil.isNull(response))
        {
            log.error(AgentConstants.CHAT_TRACE_LOG_MARKER+":{},{} run chatCompletion failed.",chatDTO.getChatId(),getName());
        }
        String jsonData=String.valueOf(response.getChoices().get(0).getMessage().getContent());

        return SubAgentResponse.builder()
                .agentName(getName())
                .result(JSONUtil.toBean(jsonData,CrisisIdentification.class))
                .build();
    }

    private String getSysPrompt(){
        return agentHolder.getProperty(SYS_PROMPT);
    }

    private boolean preCheck(){
        if(StringUtils.isBlank(getSysPrompt())){
            log.error("Crisis identify agent:{} preCheck failed,sys prompt is empty",getName());
            return false;
        }
        return true;
    }
}
