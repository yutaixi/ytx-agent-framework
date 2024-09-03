package com.ytx.ai.agent.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequestContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String tenantCode=request.getHeader(RequestContext.TENANT_CODE_HEADER_KEY);
        String agentCode=request.getHeader(RequestContext.AGENT_CODE_HEADER_KEY);
        String openAuthToken=request.getHeader(RequestContext.OPEN_AUTH_TOKEN_HEADER_KEY);
        RequestContext.setTenantCode(tenantCode);
        RequestContext.setAgentCode(agentCode);
        RequestContext.setOpenAuthToken(openAuthToken);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        RequestContext.remove();
    }
}
