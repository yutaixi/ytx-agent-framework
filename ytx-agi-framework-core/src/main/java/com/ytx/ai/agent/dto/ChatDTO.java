package com.ytx.ai.agent.dto;

import com.plexpt.chatgpt.entity.chat.Message;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class ChatDTO {

    String question;
    List<Message> history;
    Map<String,String> properties;
    String chatId;
}
