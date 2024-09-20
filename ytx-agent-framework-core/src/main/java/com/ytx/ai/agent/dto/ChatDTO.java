package com.ytx.ai.agent.dto;

import com.plexpt.chatgpt.entity.chat.Message;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ChatDTO {

    private String question;
    private List<Message> history;
    private Map<String,String> properties;
    private String chatId;
}
