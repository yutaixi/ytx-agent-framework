package com.ytx.ai.sandbox.test;

import cn.hutool.json.JSONUtil;
import com.ytx.ai.rust.Sandbox;
import com.ytx.ai.sandbox.Args;
import com.ytx.ai.sandbox.Output;
import org.junit.Test;

/**
 * 测试 Rust 实现的 Sandbox
 */
public class TestRustSandbox {

    @Test
    public void test_eval_js() {
        String script = "1 + 2";
        Output output = Sandbox.eval("js", script);
        System.out.println("Eval result: " + JSONUtil.toJsonStr(output.getValueMap()));
    }

    @Test
    public void test_run_js_no_arg() {
        String script = """
            // 同步函数定义
            function main({ params }) {
                const ret = {
                    intent: "greeting",
                    isClear: true,
                    count: 101
                };
                return ret;
            }
            """;

        Output output = Sandbox.run("js", script);
        System.out.println("Output string: " + output.getString("intent"));
        if (output.getBool("isClear") != null && output.getBool("isClear")) {
            System.out.println("Output boolean: " + output.get("isClear"));
        }
        System.out.println("Output number: " + output.getInt("count"));
        System.out.println("Full output: " + JSONUtil.toJsonStr(output.getValueMap()));
    }

    @Test
    public void test_run_js_with_arg() {
        String script = """
            // 同步函数定义
            function main({ params }) {
                let inputJson = params.input;
                const ret = {
                    intent: inputJson.intent,
                    isClear: true,
                    count: 101
                };
                return ret;
            }
            """;

        String inputJson = JSONUtil.toJsonStr(JSONUtil.parseObj("{\"intent\":\"troubleshooting\"}"));
        Output output = Sandbox.run("js", script, Args.of().bind("input", JSONUtil.parseObj("{\"intent\":\"troubleshooting\"}")));

        System.out.println("Output string: " + output.getString("intent"));
        if (output.getBool("isClear") != null && output.getBool("isClear")) {
            System.out.println("Output boolean: " + output.get("isClear"));
        }
        System.out.println("Output number: " + output.getInt("count"));
        System.out.println("Full output: " + JSONUtil.toJsonStr(output.getValueMap()));
    }

    @Test
    public void test_run_js_with_string_arg() {
        String script = """
            function main({ params }) {
                let inputJson = JSON.parse(params.countryAndCity);
                let condition;

                if (inputJson.country && inputJson.city) {
                    condition = 'country_ok_city_ok';
                } else if (inputJson.country && !inputJson.city) {
                    condition = 'country_no_ok_city_ok';
                } else if (!inputJson.country && !inputJson.city) {
                    condition = 'country_no_ok_city_no_ok';
                }

                const ret = {
                    country: inputJson.country,
                    city: inputJson.city,
                    condition: condition
                };
                return ret;
            }
            """;

        Output output = Sandbox.run(
                "js",
                script,
                Args.of().bind(
                        "countryAndCity",
                        "{\"country\":\"China\",\"city\":\"Shenzhen\"}"
                )
        );

        System.out.println(JSONUtil.toJsonStr(output.getValueMap()));
    }

    @Test
    public void test_string_utils() {
        String script = """
            function main({ params }) {
                const ret = {
                    isBlank: StringUtils.isBlank(params.test),
                    isNotBlank: StringUtils.isNotBlank(params.test)
                };
                return ret;
            }
            """;

        Output output1 = Sandbox.run("js", script, Args.of().bind("test", ""));
        System.out.println("Empty string - isBlank: " + output1.getBool("isBlank") + ", isNotBlank: " + output1.getBool("isNotBlank"));

        Output output2 = Sandbox.run("js", script, Args.of().bind("test", "   "));
        System.out.println("Whitespace - isBlank: " + output2.getBool("isBlank") + ", isNotBlank: " + output2.getBool("isNotBlank"));

        Output output3 = Sandbox.run("js", script, Args.of().bind("test", "hello"));
        System.out.println("Non-empty - isBlank: " + output3.getBool("isBlank") + ", isNotBlank: " + output3.getBool("isNotBlank"));
    }
}

