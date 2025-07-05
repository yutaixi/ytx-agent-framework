package com.ytx.ai.sandbox.runtime;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.ytx.ai.sandbox.Args;
import com.ytx.ai.sandbox.Output;
import com.ytx.ai.sandbox.SandboxFunctionRegister;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.util.Map;

@Slf4j
public class Sandbox {
    private static final String CALL_SCRIPT_WITH_JAVA_ARGS = """
        // 创建参数对象实例
        const args = {
            params: JSON.parse(javaParams),
        };
        // 调用 main 函数
        main(args);
        """;

    private static final String CALL_SCRIPT_NO_JAVA_ARGS = """
        // 调用 main 函数
        main({});
        """;

    private static final HostAccess HOST_ACCESS_STRATEGY = HostAccess.newBuilder()
            .allowAccessAnnotatedBy(HostAccess.Export.class)
            .allowImplementationsAnnotatedBy(HostAccess.Implementable.class)
            .build();

    public static Output eval(String language, String script) {
        return eval(language, script, null);
    }
    public static Output run(String language, String script) {
        String runScript = script + CALL_SCRIPT_NO_JAVA_ARGS;
        return eval(language, runScript, null);
    }

    public static Output run(String language, String script, Args args) {
        String runScript = script + CALL_SCRIPT_WITH_JAVA_ARGS;
        return eval(language, runScript, args);
    }

    private static Output eval(String language, String script, Args args) {
        try (Context context = Context.newBuilder(language)
                .allowHostAccess(HOST_ACCESS_STRATEGY)
                .build()) {
            if (args != null) {
                context.getBindings(language).putMember("javaParams", args.getParams());
                context.getBindings(language).putMember("javaParams", JSONUtil.toJsonStr(args.getParams()));
            }
            if (ObjectUtil.isNotEmpty(SandboxFunctionRegister.all())) {
                SandboxFunctionRegister.all().forEach(function -> {
                    context.getBindings(language).putMember(function.getName(), function);
                });
            }

            Value result = context.eval(language, script);
            Map<String, Object> resultMap = result.as(Map.class);
            String valueStr = JSONUtil.toJsonStr(resultMap);
            return Output.of(JSONUtil.toBean(valueStr, Map.class));
        } catch (Exception e) {
            log.info("run script error.", e);
        }
        return Output.of();
    }

}
