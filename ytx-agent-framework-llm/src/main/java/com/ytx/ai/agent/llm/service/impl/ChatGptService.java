package com.ytx.ai.agent.llm.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.ReasoningEffort;
import com.openai.models.ResponseFormatJsonObject;
import com.openai.models.ResponseFormatText;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.*;
import com.openai.models.embeddings.*;
import com.ytx.ai.agent.llm.constants.LLMConstants;
import com.ytx.ai.agent.llm.constants.ResponseFormatType;
import com.ytx.ai.agent.llm.service.LlmService;
import com.ytx.ai.agent.llm.callback.StreamCallback;
import com.ytx.ai.agent.llm.vo.ChatCompletionResponse;
import com.ytx.ai.agent.llm.vo.Content;
import com.ytx.ai.agent.llm.vo.LlmChatCompletion;
import com.ytx.ai.agent.llm.vo.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ChatGptService implements LlmService {

    @Autowired
    private OpenAIClient openAIClient;

    /**
     * 发起聊天补全请求并返回完整响应。
     * 消息顺序：先 systemPrompt（若有），再按 history 顺序转换各角色消息，最后追加 userPrompt。
     * history 中各角色处理逻辑：
     * - system：转为系统消息，content 转为字符串；
     * - assistant：转为助手消息，content 转为字符串；
     * - function：转为函数结果消息，使用 name 与 content（字符串）；
     * - user 及其他：转为用户消息，content 支持字符串或多模态 Content 列表。
     *
     * @param chatRequest 聊天请求，含 model、systemPrompt、history、userPrompt 等
     * @return 聊天补全响应（choices、usage 等）
     */
    @Override
    @SuppressWarnings("deprecation")
    public ChatCompletionResponse chatCompletionWithResponse(LlmChatCompletion chatRequest) {
        List<ChatCompletionMessageParam> messages = buildMessages(chatRequest);
        ChatCompletionCreateParams params = buildCreateParams(chatRequest, messages);
        ChatCompletion chatCompletionResponse = openAIClient.chat().completions().create(params);
        ObjectMapper mapper = new ObjectMapper();
        String json = "{}";
        try {
            json = mapper.writeValueAsString(chatCompletionResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return JSONUtil.toBean(json, ChatCompletionResponse.class);
    }

    private ReasoningEffort buildReasoningEffort(LlmChatCompletion chatRequest){
        ReasoningEffort reasoningEffort= switch (chatRequest.getReasoningEffort()) {
            case LlmChatCompletion.ReasoningEffort.NONE -> ReasoningEffort.NONE;
            case LlmChatCompletion.ReasoningEffort.MINIMAL -> ReasoningEffort.MINIMAL;
            case LlmChatCompletion.ReasoningEffort.LOW -> ReasoningEffort.LOW;
            case LlmChatCompletion.ReasoningEffort.MEDIUM -> ReasoningEffort.MEDIUM;
            case LlmChatCompletion.ReasoningEffort.HIGH -> ReasoningEffort.HIGH;
            case LlmChatCompletion.ReasoningEffort.XHIGH -> ReasoningEffort.XHIGH;
            case null-> null;
            default -> ReasoningEffort.MEDIUM;
        };

        return reasoningEffort;
    }

    private ChatCompletionCreateParams.ResponseFormat buildResponseFormat(LlmChatCompletion chatRequest){
        String responseFormat = chatRequest.getResponseFormat();
        // 默认 text（OpenAI 默认行为）
        ChatCompletionCreateParams.ResponseFormat format=ChatCompletionCreateParams.ResponseFormat.ofText(
                ResponseFormatText.builder().build()
        );
        if (responseFormat == null || responseFormat.isBlank()) {
            return format;
        }
        if (responseFormat.equalsIgnoreCase(ResponseFormatType.JSON_OBJECT)) {
            format = ChatCompletionCreateParams.ResponseFormat.ofJsonObject(
                    ResponseFormatJsonObject.builder().build()
            );
        }
        return format;
    }

    /**
     * 根据聊天请求构建 OpenAI 消息列表。
     * <p>
     * 顺序：先 systemPrompt（若有），再按 history 顺序转换各角色消息，最后追加 userPrompt。
     * history 中 system/assistant/function 的 content 转为字符串，user 支持字符串或多模态 Content 列表。
     *
     * @param chatRequest 聊天请求，含 systemPrompt、history、userPrompt
     * @return 用于 ChatCompletionCreateParams 的 messages 列表，非 null
     */
    private List<ChatCompletionMessageParam> buildMessages(LlmChatCompletion chatRequest) {
        List<ChatCompletionMessageParam> messages = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(chatRequest.getSystemPrompt())) {
            messages.add(ChatCompletionMessageParam.ofSystem(
                    ChatCompletionSystemMessageParam.builder()
                            .content(chatRequest.getSystemPrompt())
                            .build()
            ));
        }
        if (ObjectUtil.isNotEmpty(chatRequest.getHistory())) {
            for (Message item : chatRequest.getHistory()) {
                String role = item.getRole() != null ? item.getRole() : "";
                switch (role) {
                    case "system":
                        messages.add(ChatCompletionMessageParam.ofSystem(
                                ChatCompletionSystemMessageParam.builder()
                                        .content(contentToString(item.getContent()))
                                        .build()
                        ));
                        break;
                    case "assistant":
                        messages.add(ChatCompletionMessageParam.ofAssistant(
                                ChatCompletionAssistantMessageParam.builder()
                                        .content(contentToString(item.getContent()))
                                        .build()
                        ));
                        break;
                    case "function":
                        messages.add(ChatCompletionMessageParam.ofFunction(
                                ChatCompletionFunctionMessageParam.builder()
                                        .name(item.getName() != null ? item.getName() : "")
                                        .content(contentToString(item.getContent()))
                                        .build()
                        ));
                        break;
                    case "user":
                    default:
                        messages.add(ChatCompletionMessageParam.ofUser(
                                ChatCompletionUserMessageParam.builder()
                                        .content(buildUserContent(item.getContent()))
                                        .build()
                        ));
                        break;
                }
            }
        }
        if (ObjectUtil.isNotEmpty(chatRequest.getUserPrompt())) {
            messages.add(ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder()
                            .content(buildUserContent(chatRequest.getUserPrompt()))
                            .build()
            ));
        }
        return messages;
    }

    /**
     * 根据聊天请求和已构建的消息列表，构建 OpenAI 创建补全请求参数。
     *
     * @param chatRequest 聊天请求，含 model、temperature、responseFormat、reasoningEffort
     * @param messages    已构建的消息列表，由 {@link #buildMessages(LlmChatCompletion)} 得到
     * @return ChatCompletionCreateParams，用于 create 或 createStreaming
     */
    private ChatCompletionCreateParams buildCreateParams(LlmChatCompletion chatRequest,
                                                         List<ChatCompletionMessageParam> messages) {
        return ChatCompletionCreateParams.builder()
                .messages(messages)
                .model(chatRequest.getModel())
                .temperature(chatRequest.getTemperature())
                .responseFormat(buildResponseFormat(chatRequest))
                .reasoningEffort(buildReasoningEffort(chatRequest))
                .build();
    }

    /**
     * 将 system/assistant/function 消息的 content 转为字符串。
     * 用于构建仅支持文本内容的系统、助手、函数角色消息。
     *
     * @param contentObj 消息内容，可为 null、String 或其他类型
     * @return 字符串内容，null 或非字符串类型会转为空串或 JSON 字符串
     */
    private String contentToString(Object contentObj) {
        if (contentObj == null) {
            return "";
        }
        if (contentObj instanceof String) {
            return (String) contentObj;
        }
        return JSONUtil.toJsonStr(contentObj);
    }

    /**
     * 构建用户消息的 content（支持纯文本或多模态 content 数组）。
     *
     * @param contentObj 用户消息内容，可为 String 或 List&lt;Content&gt;（多模态）
     * @return SDK 所需的用户消息 Content
     */
    private ChatCompletionUserMessageParam.Content buildUserContent(Object contentObj) {

        // 情况 1：纯字符串
        if (contentObj instanceof String) {
            return ChatCompletionUserMessageParam.Content.ofText((String) contentObj);
        }

        // 情况 2：多模态 List<Content>
        if (contentObj instanceof List<?>) {

            @SuppressWarnings("unchecked")
            List<Content> contents = (List<Content>) contentObj;

            List<ChatCompletionContentPart> parts = new ArrayList<>();

            for (Content c : contents) {
                if (Content.Type.TEXT.equals(c.getType())) {
                    parts.add(ChatCompletionContentPart.ofText(
                            ChatCompletionContentPartText.builder().text(c.getText()).build()
                    ));
                }

                if (Content.Type.IMAGE_URL.equals(c.getType())) {
                    parts.add(ChatCompletionContentPart.ofImageUrl(
                            ChatCompletionContentPartImage.builder().imageUrl(
                                    ChatCompletionContentPartImage.ImageUrl.builder().url(c.getImageUrl().getUrl()).build()
                            ).build()
                    ));
                }
            }

            return ChatCompletionUserMessageParam.Content.ofArrayOfContentParts(parts);
        }

        throw new IllegalArgumentException("Unsupported content type: " + contentObj);
    }


    /**
     * Generate embeddings for the given content using OpenAI Java SDK.
     *
     * @param content The text content to generate embeddings for.
     * @return A list of float values representing the embedding.
     */
    @Override
    public List<Float> createEmbeddings(String content){
        if (ObjectUtil.isEmpty(content)) {
            log.warn("Content is empty, returning empty embeddings");
            return new ArrayList<>();
        }
        try {
            // Build the embedding request parameters
            // Using "text-embedding-ada-002" as the default model
            EmbeddingCreateParams params = EmbeddingCreateParams.builder()
                    .input(content)
                    .model(LLMConstants.DEFAULT_EMBEDDING_MODEL)
                    .build();
            // Call the OpenAI API to create embeddings
            CreateEmbeddingResponse response = openAIClient.embeddings().create(params);

            // Check and extract the embedding
            if (response.data() != null && !response.data().isEmpty()) {
                // OpenAI API typically returns one embedding for one input string
                Embedding embedding = response.data().getFirst();
                return embedding.embedding();
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
        ChatCompletionResponse response= chatCompletionWithResponse(llmChatCompletion);
        return response.getChoices().getFirst().getMessage().getContent();
    }

    /**
     * 使用 OpenAI Java SDK 的流式接口实现大模型流式对话。
     * <p>
     * 设计说明：
     * <ul>
     *     <li>内部首先构建与 {@link #chatCompletionWithResponse(LlmChatCompletion)} 一致的消息列表与参数。</li>
     *     <li>调用 OpenAI 的 streaming 能力（例如 {@code openAIClient.chat().completions().createStreaming(params)}）。</li>
     *     <li>遍历每个 {@code ChatCompletionChunk}，将其中新增的文本内容提取出来，增量回调 {@link StreamCallback#onDelta(String)}。</li>
     *     <li>流式结束后，将所有增量内容拼接成完整字符串，回调 {@link StreamCallback#onCompleted(String)}。</li>
     *     <li>异常时捕获并回调 {@link StreamCallback#onError(Throwable)}，同时记录日志。</li>
     * </ul>
     *
     * 注意：本实现依赖当前 OpenAI Java SDK 的 streaming API，若 SDK 版本发生变更，可能需要同步调整提取内容的方式。
     *
     * @param chatRequest 大模型对话补全请求
     * @param callback    流式回调接口，用于将增量内容透传给上层
     */
    @Override
    @SuppressWarnings("deprecation")
    public void chatCompletionStream(LlmChatCompletion chatRequest, StreamCallback callback) {
        // 如果未传入回调，则直接降级为普通非流式调用
        if (callback == null) {
            chatCompletion(chatRequest);
            return;
        }

        List<ChatCompletionMessageParam> messages = buildMessages(chatRequest);
        ChatCompletionCreateParams params = buildCreateParams(chatRequest, messages);

        StringBuilder fullTextBuilder = new StringBuilder();
        try (StreamResponse<ChatCompletionChunk> streamResponse =
                     openAIClient.chat().completions().createStreaming(params)) {
            // StreamResponse 非 Iterable，需通过 stream() 获取流后遍历
            streamResponse.stream().forEach(chunk -> {
                if (chunk == null || chunk.choices() == null || chunk.choices().isEmpty()) {
                    return;
                }
                for (ChatCompletionChunk.Choice choice : chunk.choices()) {
                    if (choice.delta() == null) {
                        continue;
                    }
                    // delta().content() 在 openai-java 4.x 中为 Optional<String>
                    String deltaText = choice.delta().content().orElse("");
                    if (!deltaText.isEmpty()) {
                        fullTextBuilder.append(deltaText);
                        callback.onDelta(deltaText);
                    }
                }
            });

            String fullText = fullTextBuilder.toString();
            callback.onCompleted(fullText);
        } catch (Exception e) {
            log.error("Failed to stream chat completion", e);
            callback.onError(e);
        }
    }
}
