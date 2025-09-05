package com.xrcgs.auth.security;

import com.xrcgs.common.cache.AuthCacheService;
import jakarta.annotation.Resource;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.core.Authentication;

/**
 * iam角色--表达式处理器
 */
public class IamMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {
    @Resource
    private AuthCacheService authCacheService;

    @Override
    protected IamSecurityExpressionRoot createSecurityExpressionRoot(Authentication authentication,
                                                                     MethodInvocation invocation) {
        IamSecurityExpressionRoot root = new IamSecurityExpressionRoot(authentication, authCacheService);
        root.setThis(invocation.getThis());
        return root;
    }
}
