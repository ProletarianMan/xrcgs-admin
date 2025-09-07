package com.xrcgs.auth.security;

import com.xrcgs.common.cache.AuthCacheService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Bean for checking permissions in PreAuthorize expressions.
 */
@Component("permChecker")
public class PermChecker {

    private final AuthCacheService authCacheService;

    public PermChecker(AuthCacheService authCacheService) {
        this.authCacheService = authCacheService;
    }

    /**
     * Check whether the given authentication has the specified permission.
     *
     * @param authentication current authentication
     * @param targetPerm     permission to check
     * @return true if permitted
     */
    public boolean hasPerm(Authentication authentication, String targetPerm) {
        IamSecurityExpressionRoot root = new IamSecurityExpressionRoot(authentication, authCacheService);
        return root.hasPerm(targetPerm);
    }
}

