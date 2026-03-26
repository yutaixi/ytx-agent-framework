package com.ytx.ai.web.Interceptor;

import com.ytx.ai.base.tenant.TenantContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 请求上下文拦截器
 * 在每个请求处理前，从 HTTP Header 中提取租户标识、Agent 标识、鉴权 Token，
 * 分别写入 RequestContext（Web 层使用）和 TenantContextHolder（DAO 层使用）。
 * 请求完成后统一清理，防止线程池复用时数据污染。
 */
public class RequestContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantCode = request.getHeader(RequestContext.TENANT_CODE_HEADER_KEY);
        String agentCode = request.getHeader(RequestContext.AGENT_CODE_HEADER_KEY);
        String openAuthToken = request.getHeader(RequestContext.OPEN_AUTH_TOKEN_HEADER_KEY);

        // 写入 Web 层上下文（供 Controller/Service 直接读取）
        RequestContext.setTenantCode(tenantCode);
        RequestContext.setAgentCode(agentCode);
        RequestContext.setOpenAuthToken(openAuthToken);

        // 同步写入 DAO 层租户上下文（供 MyBatis-Plus 租户插件读取）
        TenantContextHolder.setTenantCode(tenantCode);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理 Web 层上下文
        RequestContext.remove();
        // 清理 DAO 层租户上下文
        TenantContextHolder.clear();
    }
}