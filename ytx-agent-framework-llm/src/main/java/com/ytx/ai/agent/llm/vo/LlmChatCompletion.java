package com.ytx.ai.agent.llm.vo;


import cn.hutool.core.collection.ListUtil;
import com.plexpt.chatgpt.entity.chat.Message;
import com.plexpt.chatgpt.entity.chat.ResponseFormat;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatCompletion {

    private String model;
    private String systemPrompt;
    private Object userPrompt;
    private List<Message> history;
    private Double temperature;
    private String responseFormat;

    public static LlmChatCompletionBuilder builder() {
        return new LlmChatCompletionBuilder();
    }

    public static class LlmChatCompletionBuilder {
        private String model;
        private String systemPrompt;
        private Object userPrompt;
        private List<Message> history;
        private Double temperature;
        private String responseFormat;

        public LlmChatCompletionBuilder() {
        }

        public LlmChatCompletionBuilder model(final String model) {
            this.model = model;
            return this;
        }

        public LlmChatCompletionBuilder systemPrompt(final String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public LlmChatCompletionBuilder userPrompt(final String userPrompt) {
            this.userPrompt = userPrompt;
            return this;
        }

        public LlmChatCompletionBuilder userPrompt(final Content... contents) {
            this.userPrompt = ListUtil.toList(contents);
            return this;
        }

        public LlmChatCompletionBuilder history(final List<Message> history) {
            this.history = history;
            return this;
        }

        public LlmChatCompletionBuilder temperature(final Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public LlmChatCompletionBuilder jsonFormat(final boolean jsonFormat) {
            this.responseFormat = ResponseFormat.Type.JSON_OBJECT.getValue();
            return this;
        }

        public LlmChatCompletionBuilder jsonFormat() {
            this.responseFormat = ResponseFormat.Type.JSON_OBJECT.getValue();
            return this;
        }

        public LlmChatCompletion build() {
            return new LlmChatCompletion(
                    this.model,
                    this.systemPrompt,
                    this.userPrompt,
                    this.history,
                    this.temperature,
                    this.responseFormat
            );
        }
    }
}

