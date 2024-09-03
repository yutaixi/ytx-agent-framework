package com.ytx.ai.agent.skill;

import com.ytx.ai.llm.service.LlmService;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseAgent implements AiAgent{

    @Autowired
    protected LlmService llmService;

    protected Double CHAT_TEMPERATURE=0.7;

}
