package com.ytx.ai.workflow.tools;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * OAuth2 Token 管理器
 * 负责 token 的获取、缓存和刷新
 */
@Slf4j
public class OAuth2TokenManager {

    private static final int DEFAULT_TIMEOUT = 60;
    private static final long TOKEN_REFRESH_BUFFER_SECONDS = 300; // 5分钟

    // Token 缓存,key为tokenUrl + clientId + clientSecret + scope 的组合
    private static final Map<String, TokenCache> tokenCacheMap = new ConcurrentHashMap<>();
    // 每个缓存的锁,避免并发刷新
    private static final Map<String, ReentrantLock> tokenLockMap = new ConcurrentHashMap<>();

    public OAuth2TokenManager() {
    }

    /**
     * 获取 OAuth2 token,支持缓存
     */
    public String getToken(String tokenUrl, String oauth2Type, String clientId, String clientSecret,
                          String scope, String oauth2Method, Map<String, String> headers) {
        try {
            if (ObjectUtil.isEmpty(tokenUrl)) {
                log.warn("OAuth2 tokenUrl is empty, skip fetching token");
                return null;
            }

            // 目前仅支持 client_credentials
            if (ObjectUtil.isNotEmpty(oauth2Type) && !"client_credentials".equalsIgnoreCase(oauth2Type)) {
                log.warn("Unsupported oauth2Type: {}, only client_credentials is supported now", oauth2Type);
                return null;
            }

            if (ObjectUtil.isEmpty(clientId) || ObjectUtil.isEmpty(clientSecret)) {
                log.warn("clientId or clientSecret is empty, skip fetching OAuth2 token");
                return null;
            }

            // 生成缓存 key
            String cacheKey = generateCacheKey(tokenUrl, clientId, clientSecret, scope);

            // 检查缓存
            TokenCache cachedToken = tokenCacheMap.get(cacheKey);
            if (cachedToken != null && !cachedToken.isExpiredOrNearExpiry()) {
                log.debug("Using cached OAuth2 token for key: {}", cacheKey);
                return cachedToken.getToken();
            }

            // 获取或创建锁,避免并发刷新
            ReentrantLock lock = tokenLockMap.computeIfAbsent(cacheKey, k -> new ReentrantLock());
            lock.lock();
            try {
                // 双重检查,可能在等待锁的过程中其他线程已经刷新了 token
                cachedToken = tokenCacheMap.get(cacheKey);
                if (cachedToken != null && !cachedToken.isExpiredOrNearExpiry()) {
                    log.debug("Using cached OAuth2 token after lock acquisition for key: {}", cacheKey);
                    return cachedToken.getToken();
                }

                // 刷新 token
                log.info("Refreshing OAuth2 token for key: {}", cacheKey);
                return refreshToken(tokenUrl, clientId, clientSecret, scope, oauth2Method, headers, cacheKey);
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error("Error while fetching OAuth2 token", e);
            throw new RuntimeException("Error while fetching OAuth2 token: " + e.getMessage(), e);
        }
    }

    /**
     * 刷新 OAuth2 token 并更新缓存
     */
    private String refreshToken(String tokenUrl, String clientId, String clientSecret, String scope,
                                String oauth2Method, Map<String, String> headers, String cacheKey) {
        try {
            // 获取 oauth2Method,默认为 POST
            if (ObjectUtil.isEmpty(oauth2Method)) {
                oauth2Method = "POST";
            }
            oauth2Method = oauth2Method.toUpperCase();

            // 构建参数
            String params = buildOAuth2Params(clientId, clientSecret, scope);

            // 创建 HTTP 客户端
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                    .build();

            // 构建请求
            HttpRequest tokenRequest = buildTokenRequest(tokenUrl, oauth2Method, params, headers);

            // 发送请求
            HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

            // 验证响应
            validateTokenResponse(tokenResponse);

            // 解析响应并更新缓存
            return parseAndCacheToken(tokenResponse.body(), cacheKey);
        } catch (Exception e) {
            log.error("Error while refreshing OAuth2 token", e);
            throw new RuntimeException("Error while refreshing OAuth2 token: " + e.getMessage(), e);
        }
    }

    /**
     * 构建 OAuth2 请求参数
     */
    private String buildOAuth2Params(String clientId, String clientSecret, String scope) {
        StringBuilder paramBuilder = new StringBuilder();
        paramBuilder.append("grant_type=client_credentials");
        paramBuilder.append("&client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8));
        paramBuilder.append("&client_secret=").append(URLEncoder.encode(clientSecret, StandardCharsets.UTF_8));
        if (ObjectUtil.isNotEmpty(scope)) {
            paramBuilder.append("&scope=").append(URLEncoder.encode(scope, StandardCharsets.UTF_8));
        }
        return paramBuilder.toString();
    }

    /**
     * 构建 token 请求
     */
    private HttpRequest buildTokenRequest(String tokenUrl, String method, String params, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT));

        URI requestUri;
        HttpRequest.BodyPublisher bodyPublisher;

        if ("GET".equals(method)) {
            // GET方法:将参数拼接到 URL 中
            String queryString = params;
            String separator = tokenUrl.contains("?") ? "&" : "?";
            requestUri = URI.create(tokenUrl + separator + queryString);
            bodyPublisher = HttpRequest.BodyPublishers.noBody();
        } else {
            // POST, PUT, PATCH 等方法:将参数放在请求体中
            requestUri = URI.create(tokenUrl);
            bodyPublisher = HttpRequest.BodyPublishers.ofString(params);
            builder.header("Content-Type", "application/x-www-form-urlencoded");
        }

        builder.uri(requestUri);

        // 根据方法设置请求
        switch (method) {
            case "GET":
                builder.GET();
                break;
            case "POST":
                builder.POST(bodyPublisher);
                break;
            case "PUT":
                builder.PUT(bodyPublisher);
                break;
            case "PATCH":
                builder.method("PATCH", bodyPublisher);
                break;
            default:
                log.warn("Unsupported oauth2Method: {}, using POST as default", method);
                builder.POST(bodyPublisher);
                break;
        }

        // 透传除 Authorization 外的头信息
        if (ObjectUtil.isNotEmpty(headers)) {
            headers.forEach((k, v) -> {
                if (!"authorization".equalsIgnoreCase(k)) {
                    builder.header(k, v);
                }
            });
        }

        return builder.build();
    }

    /**
     * 验证 token 响应
     */
    private void validateTokenResponse(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("Fetch OAuth2 token failed, status: {}, body: {}", response.statusCode(), response.body());
            throw new RuntimeException("Fetch OAuth2 token failed, status: " + response.statusCode());
        }

        String body = response.body();
        if (ObjectUtil.isEmpty(body)) {
            log.error("Fetch OAuth2 token failed, response body is empty");
            throw new RuntimeException("Fetch OAuth2 token failed, response body is empty");
        }
    }

    /**
     * 解析响应并更新缓存
     */
    private String parseAndCacheToken(String responseBody, String cacheKey) {
        JSONObject jsonResponse = JSONUtil.parseObj(responseBody);

        String accessToken = null;
        Long expiresIn = null;
        JSONObject tokenData = null;

        // 先检查是否有 data 字段(嵌套格式)
        if (jsonResponse.containsKey("data") && jsonResponse.get("data") != null) {
            tokenData = jsonResponse.getJSONObject("data");
        }

        if (tokenData != null) {
            accessToken = tokenData.getStr("access_token");
            expiresIn = parseExpiresIn(tokenData.get("expires_in"));
        }

        // 如果没有从 data 中获取到,则尝试从根级别获取(直接格式)
        if (ObjectUtil.isEmpty(accessToken)) {
            accessToken = jsonResponse.getStr("access_token");
            expiresIn = parseExpiresIn(jsonResponse.get("expires_in"));
        }

        if (ObjectUtil.isEmpty(accessToken)) {
            log.error("Fetch OAuth2 token failed, access_token not found in response: {}", responseBody);
            throw new RuntimeException("Fetch OAuth2 token failed, access_token not found");
        }

        // 更新缓存
        if (expiresIn != null && expiresIn > 0) {
            long expireTime = System.currentTimeMillis() + (expiresIn * 1000);
            TokenCache tokenCache = new TokenCache(accessToken, expireTime);
            tokenCacheMap.put(cacheKey, tokenCache);
            log.debug("Cached OAuth2 token for key: {}, expires in {} seconds", cacheKey, expiresIn);
        } else {
            log.warn("No expires_in found in OAuth2 response, token will not be cached");
        }

        return accessToken;
    }

    /**
     * 解析 expires_in 字段
     */
    private Long parseExpiresIn(Object expiresInObj) {
        if (expiresInObj == null) {
            return null;
        }
        try {
            return Long.parseLong(expiresInObj.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse expires_in: {}", expiresInObj);
            return null;
        }
    }

    /**
     * 生成缓存 key
     */
    private String generateCacheKey(String tokenUrl, String clientId, String clientSecret, String scope) {
        return String.join("|",
                tokenUrl != null ? tokenUrl : "",
                clientId != null ? clientId : "",
                clientSecret != null ? clientSecret : "",
                scope != null ? scope : "");
    }

    /**
     * Token 缓存类
     */
    @Getter
    @Setter
    private static class TokenCache {
        private String token;
        private long expireTime; // 过期时间戳(毫秒)

        public TokenCache(String token, long expireTime) {
            this.token = token;
            this.expireTime = expireTime;
        }

        /**
         * 检查 token 是否已过期或即将过期(5分钟内)
         */
        public boolean isExpiredOrNearExpiry() {
            long currentTime = System.currentTimeMillis();
            return currentTime >= (expireTime - TOKEN_REFRESH_BUFFER_SECONDS * 1000);
        }
    }
}
