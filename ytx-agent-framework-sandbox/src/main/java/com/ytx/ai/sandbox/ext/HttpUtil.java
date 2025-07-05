package com.ytx.ai.sandbox.ext;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytx.ai.sandbox.SandboxFunction;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.graalvm.polyglot.HostAccess;


import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpUtil implements SandboxFunction {

    public static final String NAME="HttpUtil";

    @Override
    @HostAccess.Export
    public String getName() {
        return NAME;
    }
    private static final int DEFAULT_TIME_OUT=60;

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    @HostAccess.Export
    public String get(String url) {
        return get(url, DEFAULT_TIME_OUT);
    }

    @HostAccess.Export
    public String get(String url, Integer timeout) {
        Request request = new Request.Builder().url(url).get().build();
        return execute(request, timeout);
    }

    @HostAccess.Export
    public String get(String url, Map<String, String> headers) {
        return get(url, headers, DEFAULT_TIME_OUT);
    }

    @HostAccess.Export
    public String get(String url, Map<String, String> headers, Integer timeout) {
        Request.Builder requestBuilder = new Request.Builder().url(url).get();

        // 添加请求头
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        Request request = requestBuilder.build();
        return execute(request, timeout);
    }

    @HostAccess.Export
    public String post(String url, JSONObject content) {
        return post(url, content, DEFAULT_TIME_OUT);
    }

    @HostAccess.Export
    public String post(String url, JSONObject content, Integer timeout) {
        RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, JSONUtil.toJsonStr(content));
        Request request = new Request.Builder().url(url).post(body).build();
        return execute(request, timeout);
    }

    @HostAccess.Export
    public String post(String url, JSONObject content, Map<String, String> headers) {
        return post(url, content, headers, DEFAULT_TIME_OUT);
    }

    @HostAccess.Export
    public String post(String url, JSONObject content, Map<String, String> headers, Integer timeout) {
        RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, JSONUtil.toJsonStr(content));
        Request.Builder requestBuilder = new Request.Builder().url(url).post(body);

        // 添加请求头
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        Request request = requestBuilder.build();
        return execute(request, timeout);
    }

    @HostAccess.Export
    private String execute(Request request, Integer timeout) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS) // 连接超时时间 60 秒
                .readTimeout(timeout, TimeUnit.SECONDS)    // 读取超时时间 60 秒
                .writeTimeout(timeout, TimeUnit.SECONDS)   // 写入超时时间 60 秒
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Unexpected code " + response);
                throw new IOException("Unexpected code " + response);
            }

            String str = response.body().string();
            return str;
        } catch (IOException e) {
            log.error("execute failed", e);
            return "";
        }
    }


}
