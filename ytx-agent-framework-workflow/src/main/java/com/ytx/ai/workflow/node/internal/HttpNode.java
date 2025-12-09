package com.ytx.ai.workflow.node.internal;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.annotation.DependsRef;
import com.ytx.ai.workflow.annotation.DependsVariable;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.node.BasicNode;
import com.ytx.ai.workflow.node.NodeOutput;
import com.ytx.ai.workflow.tools.AuthHandler;
import com.ytx.ai.workflow.tools.OAuth2TokenManager;
import com.ytx.ai.workflow.util.ValueUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HTTP 插件
 * 负责执行 HTTP 请求
 */
@Slf4j
public class HttpNode extends BasicNode {

    private static final int DEFAULT_TIMEOUT = 60;

    // 组件依赖
    private final OAuth2TokenManager oauth2TokenManager;
    private final AuthHandler authHandler;
    private final HttpClient httpClient;

    public HttpNode() {
        this.oauth2TokenManager = new OAuth2TokenManager();
        this.authHandler = new AuthHandler(oauth2TokenManager);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                .build();
    }

    @Override
    public String getType() {
        return WorkflowPluginTypeIdEnum.HTTP.getType();
    }

    @Override
    public NodeOutput doBiz(FlowNode flowNode, FlowContext flowContext) {
        HttpPluginMeta meta = (HttpPluginMeta) flowNode.getMeta();
        try {
            // 解析嵌套的 Value 字段(HttpPlugin 特定逻辑)
            resolveNestedValues(meta, flowContext);

            // 构建请求头
            Map<String, String> headers = buildHeaders(meta.getHeaders());

            // 构建 HTTP 请求
            HttpRequest request = buildHttpRequest(meta, headers);

            // 发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 处理响应
            NodeOutput output = handleResponse(response, meta.getOutputs());

            log.debug("HTTP request completed: nodeId={}, type={}, output={}", flowNode.getId(), getType(), JSONUtil.toJsonStr(output.getData()));
            return output;
        } catch (Exception e) {
            log.error("HTTP request failed", e);
            throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    /**
     * 处理 HTTP 响应并转换为 PluginOutput
     */
    public NodeOutput handleResponse(HttpResponse<String> response, List<Value> outputs) {
        NodeOutput output = NodeOutput.of();
        if (ObjectUtil.isEmpty(outputs)) {
            return output;
        }

        for (Value outputValue : outputs) {
            String name = outputValue.getName();
            Object content = extractContent(response, name);
            if (content != null) {
                outputValue.setContent(content);
                output.addData(name, outputValue);
            }
        }
        return output;
    }

    /**
     * 解析嵌套在 headers、formData、authConfig 中的 Value 字段
     * 这是 HttpPlugin 特定的逻辑,不适合提取为通用组件
     */
    private void resolveNestedValues(HttpPluginMeta meta, FlowContext flowContext) {
        // 解析 headers 中的 value
        if (ObjectUtil.isNotEmpty(meta.getHeaders())) {
            for (HeaderItem header : meta.getHeaders()) {
                if (header.getValue() != null) {
                    resolveValue(header.getValue(), meta, flowContext);
                }
            }
        }

        // 解析 formData 中的 value
        if (ObjectUtil.isNotEmpty(meta.getFormData())) {
            for (FormDataItem formData : meta.getFormData()) {
                if (formData.getValue() != null) {
                    resolveValue(formData.getValue(), meta, flowContext);
                }
            }
        }

        // 解析 authConfig 中的 Value 字段
        if (meta.getAuthConfig() != null) {
            AuthConfig authConfig = meta.getAuthConfig();
            resolveAuthConfigValues(authConfig, meta, flowContext);
        }
    }

    /**
     * 解析单个 Value
     */
    private void resolveValue(Value value, HttpPluginMeta meta, FlowContext flowContext) {
        ValueUtils.resolveRefValue(value, flowContext);
        Map<String, Value> inputsMap = meta.getInputs() != null ? meta.getInputs().stream().collect(Collectors.toMap(Value::getName, v -> v)) : new HashMap<>();
        ValueUtils.resolveVariable(value, inputsMap, flowContext);
    }

    /**
     * 解析 AuthConfig 中的所有 Value 字段
     */
    private void resolveAuthConfigValues(AuthConfig authConfig, HttpPluginMeta meta, FlowContext flowContext) {
        Map<String, Value> inputsMap = meta.getInputs() != null ? meta.getInputs().stream().collect(Collectors.toMap(Value::getName, v -> v)) : new HashMap<>();

        if (authConfig.getUsername() != null) {
            ValueUtils.resolveRefValue(authConfig.getUsername(), flowContext);
            ValueUtils.resolveVariable(authConfig.getUsername(), inputsMap, flowContext);
        }
        if (authConfig.getPassword() != null) {
            ValueUtils.resolveRefValue(authConfig.getPassword(), flowContext);
            ValueUtils.resolveVariable(authConfig.getPassword(), inputsMap, flowContext);
        }
        if (authConfig.getToken() != null) {
            ValueUtils.resolveRefValue(authConfig.getToken(), flowContext);
            ValueUtils.resolveVariable(authConfig.getToken(), inputsMap, flowContext);
        }
        if (authConfig.getClientId() != null) {
            ValueUtils.resolveRefValue(authConfig.getClientId(), flowContext);
            ValueUtils.resolveVariable(authConfig.getClientId(), inputsMap, flowContext);
        }
        if (authConfig.getClientSecret() != null) {
            ValueUtils.resolveRefValue(authConfig.getClientSecret(), flowContext);
            ValueUtils.resolveVariable(authConfig.getClientSecret(), inputsMap, flowContext);
        }
        if (authConfig.getTokenUrl() != null) {
            ValueUtils.resolveRefValue(authConfig.getTokenUrl(), flowContext);
            ValueUtils.resolveVariable(authConfig.getTokenUrl(), inputsMap, flowContext);
        }
        if (authConfig.getScope() != null) {
            ValueUtils.resolveRefValue(authConfig.getScope(), flowContext);
            ValueUtils.resolveVariable(authConfig.getScope(), inputsMap, flowContext);
        }
        if (authConfig.getCustomHeaderValue() != null) {
            ValueUtils.resolveRefValue(authConfig.getCustomHeaderValue(), flowContext);
            ValueUtils.resolveVariable(authConfig.getCustomHeaderValue(), inputsMap, flowContext);
        }
    }

    /**
     * 从 Value 对象中获取字符串值
     */
    private String getStringValue(Value value) {
        if (value == null) {
            return null;
        }
        Object content = value.getContent();
        return content != null ? content.toString() : null;
    }

    /**
     * 构建请求头 Map
     */
    private Map<String, String> buildHeaders(List<HeaderItem> headers) {
        Map<String, String> headerMap = new HashMap<>();
        if (ObjectUtil.isNotEmpty(headers)) {
            for (HeaderItem header : headers) {
                String key = header.getKey();
                String value = getStringValue(header.getValue());
                if (ObjectUtil.isNotEmpty(key) && ObjectUtil.isNotEmpty(value)) {
                    headerMap.put(key, value);
                }
            }
        }
        return headerMap;
    }

    /**
     * 构建 HTTP 请求
     */
    private HttpRequest buildHttpRequest(HttpPluginMeta meta, Map<String, String> headers) {
        String url = getStringValue(meta.getUrl());
        if (ObjectUtil.isEmpty(url)) {
            throw new IllegalArgumentException("URL is required");
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT));

        // 设置请求头
        if (ObjectUtil.isNotEmpty(headers)) {
            headers.forEach(requestBuilder::header);
        }

        // 处理认证
        AuthConfig authConfig = meta.getAuthConfig();
        if (authConfig != null) {
            authHandler.applyAuth(
                    requestBuilder,
                    meta.getAuthType(),
                    getStringValue(authConfig.getUsername()),
                    getStringValue(authConfig.getPassword()),
                    getStringValue(authConfig.getToken()),
                    authConfig.getOauth2Type(),
                    getStringValue(authConfig.getClientId()),
                    getStringValue(authConfig.getClientSecret()),
                    getStringValue(authConfig.getTokenUrl()),
                    getStringValue(authConfig.getScope()),
                    authConfig.getOauth2Method(),
                    authConfig.getCustomHeaderName(),
                    getStringValue(authConfig.getCustomHeaderValue()),
                    headers
            );
        }

        // 设置请求方法和请求体
        String method = meta.getMethod() != null ? meta.getMethod().toUpperCase() : "GET";
        String bodyContent = getStringValue(meta.getBody());
        String bodyType = meta.getBodyType() != null ? meta.getBodyType() : "json";

        if ("GET".equals(method) || "DELETE".equals(method)) {
            requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            // POST, PUT, PATCH 等需要请求体
            HttpRequest.BodyPublisher bodyPublisher = buildRequestBody(bodyContent, bodyType, meta.getFormData());
            requestBuilder.method(method, bodyPublisher);

            // 如果没有设置 Content-Type,根据 bodyType 设置
            if (!headers.containsKey("Content-Type") && !headers.containsKey("content-type")) {
                setContentType(requestBuilder, bodyType);
            }
        }

        return requestBuilder.build();
    }

    /**
     * 构建请求体
     */
    private HttpRequest.BodyPublisher buildRequestBody(String bodyContent, String bodyType, List<FormDataItem> formData) {
        if (ObjectUtil.isEmpty(bodyContent) && ObjectUtil.isEmpty(formData)) {
            return HttpRequest.BodyPublishers.noBody();
        }

        if ("form".equalsIgnoreCase(bodyType) || "form-data".equalsIgnoreCase(bodyType)) {
            // 处理表单数据
            if (ObjectUtil.isNotEmpty(formData)) {
                String formBody = formData.stream()
                        .filter(item -> ObjectUtil.isNotEmpty(item.getKey()))
                        .map(item -> {
                            String key = item.getKey();
                            String value = getStringValue(item.getValue());
                            return key + "=" + (value != null ?
                                    URLEncoder.encode(value, StandardCharsets.UTF_8) : "");
                        })
                        .collect(Collectors.joining("&"));
                return HttpRequest.BodyPublishers.ofString(formBody);
            }
        }

        // 默认处理为 JSON 或文本
        if (ObjectUtil.isEmpty(bodyContent)) {
            return HttpRequest.BodyPublishers.noBody();
        }
        return HttpRequest.BodyPublishers.ofString(bodyContent);
    }

    /**
     * 设置 Content-Type 头
     */
    private void setContentType(HttpRequest.Builder requestBuilder, String bodyType) {
        if ("json".equalsIgnoreCase(bodyType)) {
            requestBuilder.header("Content-Type", "application/json");
        } else if ("form".equalsIgnoreCase(bodyType) || "form-data".equalsIgnoreCase(bodyType)) {
            requestBuilder.header("Content-Type", "application/x-www-form-urlencoded");
        } else if ("text".equalsIgnoreCase(bodyType)) {
            requestBuilder.header("Content-Type", "text/plain");
        }
    }

    /**
     * 从响应中提取指定字段的内容
     */
    private Object extractContent(HttpResponse<String> response, String fieldName) {
        switch (fieldName) {
            case "status":
                return response.statusCode();
            case "body":
                return response.body();
            case "headers":
                return extractHeaders(response);
            default:
                return null;
        }
    }

    /**
     * 提取响应头
     */
    private Map<String, String> extractHeaders(HttpResponse<String> response) {
        Map<String, String> responseHeaders = new HashMap<>();
        response.headers().map().forEach((key, values) -> {
            if (ObjectUtil.isNotEmpty(values)) {
                responseHeaders.put(key, String.join(", ", values));
            }
        });
        return responseHeaders;
    }

    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return HttpPluginMeta.class;
    }

    @Override
    public void init() {
    }

    @Getter
    @Setter
    public static class HttpPluginMeta implements NodeMeta {
        @DependsRef
        private List<Value> inputs;

        //GET, POST, PUT, DELETE
        private String method;

        @DependsVariable
        @DependsRef
        private Value url;

        @DependsVariable
        @DependsRef
        private List<HeaderItem> headers;

        @DependsVariable
        @DependsRef
        private Value body;

        private String bodyType; // json, form, form-data, text

        @DependsVariable
        @DependsRef
        private List<FormDataItem> formData;

        private String authType; // none, basic, bearer, oauth2, custom

        private AuthConfig authConfig;

        private List<Value> outputs;
    }

    @Getter
    @Setter
    public static class HeaderItem {
        private String id;
        private String key;
        private Value value;
    }

    @Getter
    @Setter
    public static class FormDataItem {
        private String id;
        private String key;
        @DependsVariable
        @DependsRef
        private Value value;
    }

    @Getter
    @Setter
    public static class AuthConfig {
        @DependsVariable
        @DependsRef
        private Value username;

        @DependsVariable
        @DependsRef
        private Value password;

        @DependsVariable
        @DependsRef
        private Value token;

        private String oauth2Type; // client_credentials, etc.

        @DependsVariable
        @DependsRef
        private Value clientId;

        @DependsVariable
        @DependsRef
        private Value clientSecret;

        @DependsVariable
        @DependsRef
        private Value tokenUrl;

        @DependsVariable
        @DependsRef
        private Value scope;

        private String customHeaderName;

        @DependsVariable
        @DependsRef
        private Value customHeaderValue;

        private String oauth2Method; // GET, POST, PUT, PATCH
    }
}
