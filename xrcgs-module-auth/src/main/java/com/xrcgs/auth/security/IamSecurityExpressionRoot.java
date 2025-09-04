package com.xrcgs.auth.security;

import com.xrcgs.common.cache.AuthCacheService;
import com.xrcgs.common.security.UserPrincipal;
import com.xrcgs.common.util.PermMatcher;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.*;

/**
 * iam模块 表达式 Root
 */
public class IamSecurityExpressionRoot extends SecurityExpressionRoot
        implements MethodSecurityExpressionOperations {

    private final AuthCacheService authCacheService;
    private Object filterObject;
    private Object returnObject;
    private Object target;

    public IamSecurityExpressionRoot(Authentication authentication, AuthCacheService authCacheService) {
        super(authentication);
        this.authCacheService = authCacheService;
    }

    /** 使用：@PreAuthorize("hasPerm('iam:user:list')") */
    public boolean hasPerm(String targetPerm) {
        Authentication auth = getAuthentication();  // ✅ 通过访问器获取

        if (isAdmin(auth == null ? null : auth.getAuthorities())) {
            return true;
        }

        Set<String> perms = extractPerms(auth == null ? null : auth.getAuthorities());
        Long userId = extractUserId(auth);

        if ((perms == null || perms.isEmpty()) && userId != null) {
            Set<String> cached = authCacheService.getCachedUserPerms(userId);
            if (cached != null) perms = cached;
        }
        return PermMatcher.match(perms, targetPerm);
    }

    /* ---------- helpers ---------- */

    private boolean isAdmin(Collection<? extends GrantedAuthority> auths){
        if (auths == null) return false;
        for (GrantedAuthority a : auths) {
            if (a != null && "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority())) return true;
        }
        return false;
    }

    private Set<String> extractPerms(Collection<? extends GrantedAuthority> auths){
        Set<String> set = new HashSet<>();
        if (auths == null) return set;
        for (GrantedAuthority a : auths) {
            if (a == null) continue;
            String s = a.getAuthority();
            if (s != null && s.startsWith("PERM_")) set.add(s.substring(5));
        }
        return set;
    }

    private Long extractUserId(Authentication auth){
        if (auth == null) return null;
        Object p = auth.getPrincipal();
        if (p instanceof UserPrincipal up) return up.getUserId();
        Object d = auth.getDetails();
        if (d instanceof UserPrincipal up2) return up2.getUserId();
        return null;
    }

    /* ---------- MethodSecurityExpressionOperations ---------- */
    @Override public void setFilterObject(Object filterObject) { this.filterObject = filterObject; }
    @Override public Object getFilterObject() { return filterObject; }
    @Override public void setReturnObject(Object returnObject) { this.returnObject = returnObject; }
    @Override public Object getReturnObject() { return returnObject; }
    @Override public Object getThis() { return target; }
    public void setThis(Object target) { this.target = target; }
}
