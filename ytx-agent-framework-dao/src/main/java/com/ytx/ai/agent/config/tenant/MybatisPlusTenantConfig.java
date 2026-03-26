package com.ytx.ai.agent.config.tenant;


import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 多租户插件配置
 * 注册 TenantLineInnerInterceptor，使所有通过 MyBatis-Plus 执行的 SQL
 * 自动注入 tenant_code 过滤条件，实现透明的行级租户数据隔离。
 *
 * 此配置会被 AgentMysqlDataSourceConfig 和 AgentPostgresqlDataSourceConfig
 * 在构建 SqlSessionFactory 时注入为插件使用。
 */
@Configuration
public class MybatisPlusTenantConfig {

    /**
     * 租户 SQL 处理器 Bean，实现了从 TenantContextHolder 读取当前租户的逻辑
     * @return TenantLineHandlerImpl 实例
     */
    @Bean
    public TenantLineHandlerImpl tenantLineHandler() {
        return new TenantLineHandlerImpl();
    }

    /**
     * MyBatis-Plus 拦截器 Bean，内含租户行级过滤插件
     * 此 Bean 会被 DataSource 配置类注入到 SqlSessionFactory，
     * 从而对所有 SQL 执行进行租户条件注入。
     * @param tenantLineHandler 租户处理器
     * @return MybatisPlusInterceptor 实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(TenantLineHandlerImpl tenantLineHandler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));
        return interceptor;
    }
}