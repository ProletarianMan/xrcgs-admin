package com.xrcgs.auth.security;

import com.xrcgs.common.cache.AuthCacheService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Method security configuration that wires a custom expression handler.
 * 连接自定义表达式处理程序的方法安全配置。
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(AuthCacheService authCacheService) {
        return new IamMethodSecurityExpressionHandler(authCacheService);
    }
}
