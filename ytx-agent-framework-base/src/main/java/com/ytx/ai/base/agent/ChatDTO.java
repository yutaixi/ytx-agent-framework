package com.ytx.ai.base.agent;

import com.ytx.ai.agent.llm.vo.Message;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ChatDTO {

    private String question;
    private List<Message> history;
    private Map<String,Object> properties;
    private String chatId;
}
