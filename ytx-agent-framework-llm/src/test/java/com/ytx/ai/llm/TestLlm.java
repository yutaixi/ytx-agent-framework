package com.ytx.ai.llm;

import cn.hutool.json.JSONUtil;
import com.ytx.ai.agent.llm.config.LlmConfigProperty;
import com.ytx.ai.agent.llm.vo.LlmChatCompletion;
import com.ytx.ai.rust.Llm;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * Llm JNI 测试类
 *
 * 注意：运行此测试前需要：
 * 1. 确保 Rust 库已编译（llm.dll / libllm.so / libllm.dylib）
 * 2. 设置正确的库路径（通过 java.library.path 或系统 PATH）
 * 3. 配置有效的 API key 和 API host
 */
@Slf4j
public class TestLlm {

    private static LlmConfigProperty config;

    @BeforeClass
    public static void setUp() {
        // 创建测试配置
        // 注意：请根据实际情况修改 API key 和 host
        config = new LlmConfigProperty();
        config.setApiKey(System.getProperty("llm.api.key", "sk-KTuxs7OcY7On67Cy9c646fD000D9422899F98325945c46D2"));
        config.setApiHost(System.getProperty("llm.api.host", "http://192.168.31.125:3000/v1"));
        config.setTimeout(20);

        log.info("测试配置: apiHost={}, timeout={}", config.getApiHost(), config.getTimeout());
    }

    @Test
    public void testChatCompletionSimple() {
        log.info("=== 测试简单聊天完成 ===");

        LlmChatCompletion request = LlmChatCompletion.builder()
                .model("gpt-4o")
                .systemPrompt("You are a helpful assistant.")
                .userPrompt("Say hello in one sentence.")
                .temperature(0.7)
                .build();

        try {
            String result = Llm.chatCompletion(config, request);

            assertNotNull("响应不应为 null", result);
            assertFalse("响应不应为空", result.isEmpty());

            log.info("简单聊天完成响应: {}", result);
        } catch (UnsatisfiedLinkError e) {
            log.error("无法加载本地库，请确保 Rust 库已编译并在库路径中: {}", e.getMessage());
            fail("无法加载本地库: " + e.getMessage());
        } catch (Exception e) {
            log.error("聊天完成失败", e);
            fail("聊天完成失败: " + e.getMessage());
        }
    }

    @Test
    public void testChatCompletionFull() {
        log.info("=== 测试完整聊天完成 ===");

        LlmChatCompletion request = LlmChatCompletion.builder()
                .model("gpt-4o")
                .systemPrompt("You are a helpful assistant.")
                .userPrompt("What is 2+2? Answer in one sentence.")
                .temperature(0.7)
                .build();

        try {
            String result = Llm.chatCompletionFull(config, request);

            assertNotNull("响应不应为 null", result);
            assertFalse("响应不应为空", result.isEmpty());

            // 验证返回的是有效的 JSON
            assertTrue("响应应该是有效的 JSON", JSONUtil.isTypeJSON(result));

            log.info("完整聊天完成响应: {}", result);

            // 解析并验证 JSON 结构
            var jsonObj = JSONUtil.parseObj(result);
            if (jsonObj.containsKey("error")) {
                log.warn("API 返回错误: {}", jsonObj.getStr("error"));
            } else {
                assertTrue("响应应包含 choices 字段", jsonObj.containsKey("choices"));
            }
        } catch (UnsatisfiedLinkError e) {
            log.error("无法加载本地库: {}", e.getMessage());
            fail("无法加载本地库: " + e.getMessage());
        } catch (Exception e) {
            log.error("完整聊天完成失败", e);
            fail("完整聊天完成失败: " + e.getMessage());
        }
    }

    @Test
    public void testChatCompletionWithHistory() {
        log.info("=== 测试带历史记录的聊天完成 ===");

        // 创建历史记录
        var history = java.util.Arrays.asList(
                new com.plexpt.chatgpt.entity.chat.Message("user", "My name is Alice"),
                new com.plexpt.chatgpt.entity.chat.Message("assistant", "Hello Alice! Nice to meet you.")
        );

        LlmChatCompletion request = LlmChatCompletion.builder()
                .model("gpt-4o")
                .systemPrompt("You are a helpful assistant.")
                .userPrompt("What is my name?")
                .history(history)
                .temperature(0.7)
                .build();

        try {
            String result = Llm.chatCompletion(config, request);

            assertNotNull("响应不应为 null", result);
            assertFalse("响应不应为空", result.isEmpty());

            log.info("带历史记录的聊天完成响应: {}", result);
        } catch (UnsatisfiedLinkError e) {
            log.error("无法加载本地库: {}", e.getMessage());
            fail("无法加载本地库: " + e.getMessage());
        } catch (Exception e) {
            log.error("带历史记录的聊天完成失败", e);
            fail("带历史记录的聊天完成失败: " + e.getMessage());
        }
    }

    @Test
    public void testCreateEmbeddings() {
        log.info("=== 测试创建嵌入向量 ===");

        String content = "Hello, world! This is a test.";
        String model = null; // 使用默认模型

        try {
            float[] embeddings = Llm.createEmbeddings(config, content, model);

            assertNotNull("嵌入向量不应为 null", embeddings);
            assertTrue("嵌入向量不应为空", embeddings.length > 0);

            log.info("嵌入向量维度: {}", embeddings.length);
            log.info("前5个值: [{}, {}, {}, {}, {}]",
                    embeddings[0],
                    embeddings.length > 1 ? embeddings[1] : 0,
                    embeddings.length > 2 ? embeddings[2] : 0,
                    embeddings.length > 3 ? embeddings[3] : 0,
                    embeddings.length > 4 ? embeddings[4] : 0);
        } catch (UnsatisfiedLinkError e) {
            log.error("无法加载本地库: {}", e.getMessage());
            fail("无法加载本地库: " + e.getMessage());
        } catch (Exception e) {
            log.error("创建嵌入向量失败", e);
            fail("创建嵌入向量失败: " + e.getMessage());
        }
    }

    @Test
    public void testCreateEmbeddingsWithModel() {
        log.info("=== 测试使用指定模型创建嵌入向量 ===");

        String content = "Test embedding with specific model";
        String model = "text-embedding-ada-002";

        try {
            float[] embeddings = Llm.createEmbeddings(config, content, model);

            assertNotNull("嵌入向量不应为 null", embeddings);
            assertTrue("嵌入向量不应为空", embeddings.length > 0);

            log.info("使用模型 {} 的嵌入向量维度: {}", model, embeddings.length);
        } catch (UnsatisfiedLinkError e) {
            log.error("无法加载本地库: {}", e.getMessage());
            fail("无法加载本地库: " + e.getMessage());
        } catch (Exception e) {
            log.error("创建嵌入向量失败", e);
            fail("创建嵌入向量失败: " + e.getMessage());
        }
    }

    @Test
    public void testChatCompletionWithJsonFormat() {
        log.info("=== 测试 JSON 格式响应 ===");

        LlmChatCompletion request = LlmChatCompletion.builder()
                .model("gpt-4o")
                .systemPrompt("You are a helpful assistant. Always respond in valid JSON format.")
                .userPrompt("Return a JSON object with two fields: 'greeting' and 'language'. Example: {\"greeting\": \"Hello\", \"language\": \"English\"}")
                .temperature(0.7)
                .jsonFormat(true)
                .build();

        try {
            String result = Llm.chatCompletionFull(config, request);

            assertNotNull("响应不应为 null", result);
            assertFalse("响应不应为空", result.isEmpty());
            assertTrue("响应应该是有效的 JSON", JSONUtil.isTypeJSON(result));

            log.info("JSON 格式响应: {}", result);
        } catch (UnsatisfiedLinkError e) {
            log.error("无法加载本地库: {}", e.getMessage());
            fail("无法加载本地库: " + e.getMessage());
        } catch (Exception e) {
            log.error("JSON 格式聊天完成失败", e);
            fail("JSON 格式聊天完成失败: " + e.getMessage());
        }
    }
}
