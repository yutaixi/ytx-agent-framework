package com.ytx.ai.agent.interceptor;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.HashMap;
import java.util.Map;

public class RequestContext {

    public static final String TENANT_CODE_HEADER_KEY="x-header-tenant";
    public static final String AGENT_CODE_HEADER_KEY="x-header-agent";
    public static final String OPEN_AUTH_TOKEN_HEADER_KEY="Authorization";

    private static final ThreadLocal<Map<String,Object>> SESSION=new TransmittableThreadLocal<>();

    public static void setTenantCode(String tenantCode){
        setValue(TENANT_CODE_HEADER_KEY,tenantCode);
    }

    public static String getTenantCode()
    {
        Object value=getValue(TENANT_CODE_HEADER_KEY);
        if(ObjectUtil.isNull(value)){
            return null;
        }
        return String.valueOf(value);
    }


    public static void setAgentCode(String agentCode){
        setValue(AGENT_CODE_HEADER_KEY,agentCode);
    }

    public static String getAgentCode()
    {
        Object value=getValue(AGENT_CODE_HEADER_KEY);
        if(ObjectUtil.isNull(value)){
            return null;
        }
        return String.valueOf(value);
    }

    public static void setOpenAuthToken(String token){
        setValue(OPEN_AUTH_TOKEN_HEADER_KEY,token);
    }

    public static String getOpenAuthToken()
    {
        Object value=getValue(OPEN_AUTH_TOKEN_HEADER_KEY);
        if(ObjectUtil.isNull(value)){
            return null;
        }
        return String.valueOf(value);
    }

    public static void setValue(String key,String value){
        Map<String,Object> sessionMap=SESSION.get();
        if(ObjectUtil.isNull(sessionMap))
        {
            sessionMap=new HashMap<>();
        }
        sessionMap.put(key,value);
        SESSION.set(sessionMap);
    }

    public static Object getValue(String key){
        Map<String,Object> sessionMap=SESSION.get();
        if(ObjectUtil.isNull(sessionMap))
        {
            return null;
        }
        return sessionMap.get(key);
    }

    public static void remove(){
        SESSION.remove();
    }
}
