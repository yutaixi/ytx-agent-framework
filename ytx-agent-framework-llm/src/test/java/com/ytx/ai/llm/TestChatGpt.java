package com.ytx.ai.llm;


import com.plexpt.chatgpt.ChatGPT;
import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
import com.plexpt.chatgpt.entity.chat.Message;
import com.plexpt.chatgpt.entity.embedding.EmbeddingResult;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class TestChatGpt {

    private ChatGPT chatGPT;

    @Before
    public  void init(){
        chatGPT = ChatGPT.builder()
                .apiKey("sk-KTuxs7OcY7On67Cy9c646fD000D9422899F98325945c46D2")
                .timeout(900)
                .apiHost("http://192.168.31.125:3000/")
                .build()
                .init();
    }

    @Test
    public void test_chat_gpt()
    {


        Message system = Message.ofSystem("你现在是一个诗人，专门写七言绝句");
        Message message = Message.of("写一段七言绝句诗，题目是：火锅！");

        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model("gpt-5")
                .messages(Arrays.asList(system, message))
                .maxTokens(3000)
                .temperature(0.7)
                .build();
        ChatCompletionResponse response = chatGPT.chatCompletion(chatCompletion);
        Message res = response.getChoices().get(0).getMessage();
        System.out.println(res);
    }


    @Test
    public void test_embedding(){

        String message="this is a test message.";
        EmbeddingResult embeddingResult=chatGPT.createEmbeddings(message,"transsion-openai");
        System.out.println(embeddingResult.getData());

    }

    @Test
    public void test_chat()
    {


        Message system = Message.ofSystem("你是一个小助手");
        Message message = Message.of("下面这个命令是什么意思：docker run --rm -v /var/run/docker.sock:/var/run/docker.sock containrrr/watchtower -cR");

        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model(ChatCompletion.Model.GPT_3_5_TURBO)
                .messages(Arrays.asList(system, message))
                .maxTokens(3000)
                .temperature(0.7)
                .build();
        ChatCompletionResponse response = chatGPT.chatCompletion(chatCompletion);
        Message res = response.getChoices().get(0).getMessage();
        System.out.println(res);
    }



}
