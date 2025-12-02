package com.ytx.ai.workflow.tools;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.http.HttpRequest;
import java.util.Base64;
import java.util.Map;

/**
 * 认证处理器 (Authentication Handler)
 * 负责处理各种认证方式 (Responsible for handling various authentication methods)
 */
@Slf4j
public class AuthHandler {

    private final OAuth2TokenManager oauth2TokenManager;

    public AuthHandler(OAuth2TokenManager oauth2TokenManager) {
        this.oauth2TokenManager = oauth2TokenManager;
    }

    /**
     * 应用认证 (Apply Authentication)
     */
    public void applyAuth(HttpRequest.Builder requestBuilder, String authType,
                          String username, String password, String token,
                          String oauth2Type, String clientId, String clientSecret,
                          String tokenUrl, String scope, String oauth2Method,
                          String customHeaderName, String customHeaderValue,
                          Map<String, String> headers) {
        if (ObjectUtil.isEmpty(authType) || "none".equals(authType)) {
            return;
        }

        switch (authType) {
            case "basic":
                applyBasicAuth(requestBuilder, username, password);
                break;
            case "bearer":
                applyBearerAuth(requestBuilder, token);
                break;
            case "oauth2":
                applyOAuth2Auth(requestBuilder, token, tokenUrl, scope, oauth2Method,
                        oauth2Type, clientId, clientSecret, headers);
                break;
            case "custom":
                applyCustomAuth(requestBuilder, customHeaderName, customHeaderValue);
                break;
        }
    }

    /**
     * 应用 Basic 认证
     */
    private void applyBasicAuth(HttpRequest.Builder requestBuilder, String username, String password) {
        if (ObjectUtil.isNotEmpty(username) && ObjectUtil.isNotEmpty(password)) {
            String credentials = username + ":" + password;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
            requestBuilder.header("Authorization", "Basic " + encodedCredentials);
        }
    }

    /**
     * 应用 Bearer 认证
     */
    private void applyBearerAuth(HttpRequest.Builder requestBuilder, String token) {
        if (ObjectUtil.isNotEmpty(token)) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }
    }

    /**
     * 应用 OAuth2 认证
     */
    private void applyOAuth2Auth(HttpRequest.Builder requestBuilder, String token,
                                 String tokenUrl, String scope, String oauth2Method,
                                 String oauth2Type, String clientId, String clientSecret,
                                 Map<String, String> headers) {
        // 优先使用已经配置好的token,否则通过 tokenUrl 申请获取
        if (ObjectUtil.isEmpty(token)) {
            token = oauth2TokenManager.getToken(tokenUrl, oauth2Type, clientId, clientSecret,
                    scope, oauth2Method, headers);
        }
        if (ObjectUtil.isNotEmpty(token)) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }
    }

    /**
     * 应用自定义认证
     */
    private void applyCustomAuth(HttpRequest.Builder requestBuilder, String customHeaderName, String customHeaderValue) {
        if (ObjectUtil.isNotEmpty(customHeaderName) && ObjectUtil.isNotEmpty(customHeaderValue)) {
            requestBuilder.header(customHeaderName, customHeaderValue);
        }
    }
}

