package com.ytx.ai.sandbox.test;

import cn.hutool.json.JSONUtil;
import com.ytx.ai.sandbox.Args;
import com.ytx.ai.sandbox.Output;
import com.ytx.ai.sandbox.SandboxFunctionRegister;
import com.ytx.ai.sandbox.runtime.Sandbox;
import com.ytx.ai.sandbox.test.ext.TestSandboxFunction;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Test;

public class TestJavascript {

    @Test
    public void test_run_js_org() {
        try (Context context = Context.create()) {
            Value result = context.eval("js", "1 + 2");
            System.out.println(result.asInt()); // 输出 3
        }
    }

    @Test
    public void test_run_js_no_arg() {
        String script = """
            // 同步函数定义
            function main({ params }) {
                let inputJson = JSON.parse(params.input);
                const ret = {
                    intent: inputJson.intent,
                    isClear: true,
                    count: 101
                };
                return ret;
            }

            // 创建参数对象实例
            const args = {
                params: {
                    input: "{ \\"intent\\": \\"greeting\\" }"
                },
            };

            // 调用 main 函数
            main(args);
            """;

        Output output = Sandbox.eval("js", script);
        System.out.println("Output string: " + output.getString("intent"));
        if (output.getBool("isClear")) {
            System.out.println("Output boolean: " + output.get("isClear"));
        }
        System.out.println("Output number: " + output.getInt("count"));
    }

    @Test
    public void test_run_js_with_arg() {
        String script = """
        // 同步函数定义
        function main({ params }) {
//            let inputJson = JSON.parse(params.input);
            let inputJson = params.input;
            const ret = {
                intent: inputJson.intent,
                isClear: true,
                count: 101
            };
            return ret;
        }
        """;

        String jsonStr = JSONUtil.toJsonStr( "{\"intent\":\"troubleshooting\"}");
        Output output = Sandbox.run( "js", script, Args.of().bind( "input", JSONUtil.parseObj( "{\"intent\":\"troubleshooting\"}")));

        System.out.println("Output string: " + output.getString( "intent"));
        if (output.getBool( "isClear")) {
            System.out.println("Output boolean: " + output.get("isClear"));
        }
        System.out.println("Output number: " + output.getInt( "count"));
    }

    @Test
    public void test_run_js_with_arg_permission() {
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
    public void test_http_request() {
        String script = """
        console.log(testFunction.run('abc'));
        console.log(testFunction.getName());
        // 同步函数定义
        function main({ params }) {
            console.log('this is code workflowNode running.');
            const ret = {
                body: 'this is body'
            };
            return ret;
        }
        """;

        SandboxFunctionRegister.register(new TestSandboxFunction());
        Output output = Sandbox.run( "js", script);
        System.out.println("Output string: " + output.getString( "body"));
    }


}
