package com.ytx.ai.agent.llm.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.plexpt.chatgpt.ChatGPT;
import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
import com.plexpt.chatgpt.entity.chat.Message;
import com.plexpt.chatgpt.entity.chat.ResponseFormat;
import com.ytx.ai.agent.llm.service.LlmService;
import com.ytx.ai.agent.llm.vo.LlmChatCompletion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ChatGptService implements LlmService {

    public static final String JSON_OBJECT="json_object";

    @Autowired
    private ChatGPT chatGPT;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Override
    public ChatCompletionResponse chatCompletion(ChatCompletion chatCompletion) {
        return chatGPT.chatCompletion(chatCompletion);
    }

    @Override
    public List<Float> createEmbeddings(String content){
        if (ObjectUtil.isEmpty(content)) {
            log.warn("Content is empty, returning empty embeddings");
            return new ArrayList<>();
        }

        try {
            // 调用嵌入模型生成嵌入向量
            EmbeddingResponse embeddingResponse = embeddingModel.embedForResponse(List.of(content));

            // 从响应中提取嵌入向量
            if (embeddingResponse != null && embeddingResponse.getResult() != null
                    && embeddingResponse.getResult().getOutput() != null) {
                // 将 float[] 数组转换为 List<Float>
                float[] output = embeddingResponse.getResult().getOutput();
                List<Float> embeddings = new ArrayList<>(output.length);
                for (float value : output) {
                    embeddings.add(Float.valueOf(value));
                }
                return embeddings;
            } else {
                log.error("Embedding response is null or empty");
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("Failed to create embeddings for content: {}", content, e);
            return new ArrayList<>();
        }
    }


    public String chatCompletion(LlmChatCompletion llmChatCompletion){
        List<Message> messages=new ArrayList<>();
        messages.add(Message.ofSystem(llmChatCompletion.getSystemPrompt()));
        if(ObjectUtil.isNotEmpty(llmChatCompletion.getHistory())){
            messages.addAll(llmChatCompletion.getHistory());
        }
        messages.add(Message.of(llmChatCompletion.getUserPrompt().toString()));

        ChatCompletion chatCompletion=ChatCompletion.builder()
                .model(llmChatCompletion.getModel())
                .messages(messages)
                .temperature(llmChatCompletion.getTemperature())
                .build();
        if(ResponseFormat.Type.JSON_OBJECT.getValue().equalsIgnoreCase(llmChatCompletion.getResponseFormat())){
            chatCompletion.setResponseFormat(ResponseFormat.builder().type(JSON_OBJECT).build());
        }
        ChatCompletionResponse response= chatGPT.chatCompletion(chatCompletion);
        if(ObjectUtil.isNull(response))
        {
            log.error("run chatCompletion failed.");
            return null;
        }
        return response.getChoices().get(0).getMessage().getContent();
    }
}
