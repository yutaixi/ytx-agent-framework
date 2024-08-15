package com.ytx.ai.base.web;

import cn.hutool.core.util.ObjectUtil;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class RequestContextUtils {

    public static HttpServletRequest getRequest()
    {

        RequestAttributes requestAttributes= RequestContextHolder.getRequestAttributes();
        if(ObjectUtil.isNull(requestAttributes)){
            return null;
        }
        ServletRequestAttributes attributes=(ServletRequestAttributes)requestAttributes;
        return ObjectUtil.isNull(attributes)?null:attributes.getRequest();
    }
}
