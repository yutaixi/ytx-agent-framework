package com.ytx.ai.rust;

import cn.hutool.json.JSONUtil;
import com.ytx.ai.base.util.JniLibraryLoader;
import com.ytx.ai.sandbox.Args;
import com.ytx.ai.sandbox.Output;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Java JNI 包装类，用于调用 Rust sandbox 库
 *
 * 该类提供了与 Rust 实现的 sandbox 引擎交互的接口
 */
@Slf4j
public class Sandbox {

    // 加载本地库
    static {
        JniLibraryLoader.load("ytx_sandbox");
    }

    /**
     * 执行脚本（eval 模式）
     *
     * @param language 脚本语言（如 "js", "javascript"）
     * @param script 脚本内容
     * @return Output 对象，包含执行结果
     */
    public static Output eval(String language, String script) {
        String resultJson = evalNative(language, script);
        if (resultJson == null) {
            return Output.of();
        }
        try {
            Map<String, Object> valueMap = JSONUtil.toBean(resultJson, Map.class);
            return Output.of(valueMap);
        } catch (Exception e) {
            log.error("Failed to parse result JSON: {}", e.getMessage(), e);
            return Output.of();
        }
    }

    /**
     * 运行脚本（无参数）
     *
     * @param language 脚本语言（如 "js", "javascript"）
     * @param script 脚本内容
     * @return Output 对象，包含执行结果
     */
    public static Output run(String language, String script) {
        String resultJson = runNative(language, script);
        if (resultJson == null) {
            return Output.of();
        }
        try {
            Map<String, Object> valueMap = JSONUtil.toBean(resultJson, Map.class);
            return Output.of(valueMap);
        } catch (Exception e) {
            log.error("Failed to parse result JSON: {}", e.getMessage(), e);
            return Output.of();
        }
    }

    /**
     * 运行脚本（带参数）
     *
     * @param language 脚本语言（如 "js", "javascript"）
     * @param script 脚本内容
     * @param args 参数对象
     * @return Output 对象，包含执行结果
     */
    public static Output run(String language, String script, Args args) {
        String argsJson = "";
        if (args != null && args.getParams() != null) {
            argsJson = JSONUtil.toJsonStr(args.getParams());
        }
        String resultJson = runWithArgsNative(language, script, argsJson);
        if (resultJson == null) {
            return Output.of();
        }
        try {
            Map<String, Object> valueMap = JSONUtil.toBean(resultJson, Map.class);
            return Output.of(valueMap);
        } catch (Exception e) {
            log.error("Failed to parse result JSON: {}", e.getMessage(), e);
            return Output.of();
        }
    }

    /**
     * 执行脚本（eval 模式）- JNI 原生方法
     */
    private static native String evalNative(String language, String script);

    /**
     * 运行脚本（无参数）- JNI 原生方法
     */
    private static native String runNative(String language, String script);

    /**
     * 运行脚本（带参数）- JNI 原生方法
     */
    private static native String runWithArgsNative(String language, String script, String argsJson);
}

