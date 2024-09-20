package com.ytx.ai.llm;


import com.plexpt.chatgpt.ChatGPT;
import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
import com.plexpt.chatgpt.entity.chat.Message;
import com.plexpt.chatgpt.entity.chat.ResponseFormat;
import org.junit.Test;

import java.util.Arrays;

public class TestChatGpt {

    private ChatGPT chatGPT;


    @Test
    public void test_chat_gpt()
    {
        chatGPT = ChatGPT.builder()
                .apiKey("model-name-router-7c7aa4a3549f12")
                .timeout(900)
                .apiHost("http://192.168.88.129:8090/")
                .build()
                .init();

        Message system = Message.ofSystem("你现在是一个诗人，专门写七言绝句");
        Message message = Message.of("写一段七言绝句诗，题目是：火锅！");

        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model(ChatCompletion.Model.GPT4o)
                .messages(Arrays.asList(system, message))
                .maxTokens(3000)
                .temperature(0.7)
                .responseFormat(ResponseFormat.builder().type("json_object").build())
                .build();
        ChatCompletionResponse response = chatGPT.chatCompletion(chatCompletion);
        Message res = response.getChoices().get(0).getMessage();
        System.out.println(res);
    }


}
