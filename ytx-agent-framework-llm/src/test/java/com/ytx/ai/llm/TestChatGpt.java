package com.ytx.ai.llm;


import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.junit.Before;

import java.time.Duration;

public class TestChatGpt {

    private OpenAIClient openAIClient;

    @Before
    public  void init(){
        openAIClient= OpenAIOkHttpClient.builder()
                .apiKey("sk-KTuxs7OcY7On67Cy9c646fD000D9422899F98325945c46D2")
                .baseUrl("http://192.168.31.125:3000/v1/")
                .timeout(Duration.ofSeconds(60))
                .build();
    }


}
