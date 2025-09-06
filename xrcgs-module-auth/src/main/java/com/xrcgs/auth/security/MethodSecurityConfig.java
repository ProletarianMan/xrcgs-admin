package com.xrcgs.auth.security;

import com.xrcgs.common.cache.AuthCacheService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.MethodSecurityConfigurer;

/**
 * Method security configuration that wires a custom expression handler.
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(AuthCacheService authCacheService) {
        return new IamMethodSecurityExpressionHandler(authCacheService);
    }

    @Bean
    public MethodSecurityConfigurer methodSecurityConfigurer(AuthCacheService authCacheService) {
        return builder -> builder.expressionHandler(methodSecurityExpressionHandler(authCacheService));
    }
}
