package com.ytx.ai.base.tenant;


import cn.hutool.core.util.ObjectUtil;
import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 多租户上下文持有者
 * 使用 ThreadLocal 存储当前请求线程的租户标识，供各层（DAO 层等）读取。
 * 由 Web 拦截器在请求进入时写入，请求结束时清理。
 */
public class TenantContextHolder {

    /** 存储当前线程的租户 code */
    private static final ThreadLocal<String> TENANT_CODE_HOLDER = new TransmittableThreadLocal<>();

    /**
     * 设置当前线程的租户 code
     * @param tenantCode 租户标识码，为 null 表示无租户上下文
     */
    public static void setTenantCode(String tenantCode) {
        if (ObjectUtil.isEmpty(tenantCode)) {
            TENANT_CODE_HOLDER.remove();
        } else {
            TENANT_CODE_HOLDER.set(tenantCode);
        }
    }

    /**
     * 获取当前线程的租户 code
     * @return 租户标识码，若未设置则返回 null
     */
    public static String getTenantCode() {
        return TENANT_CODE_HOLDER.get();
    }

    /**
     * 清除当前线程的租户上下文，防止线程复用时数据污染
     */
    public static void clear() {
        TENANT_CODE_HOLDER.remove();
    }
}