package com.xrcgs.infrastructure.audit;

import com.xrcgs.common.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * 仅从 Spring Security 获取当前用户ID（不依赖 oauth2 包）
 * 解析顺序：
 *   1) principal instanceof UserIdAware -> getId()
 *   2) principal instanceof UserDetails -> 尝试把 username 解析成 Long
 *   3) auth.getName() -> 尝试解析成 Long
 */
@Component
public class SecurityOnlyUserIdProvider implements UserIdProvider {

    @Override
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();

        // 1) JWT 过滤器会放入我们自己的 UserPrincipal，优先读取其中的 Long userId
        if (principal instanceof UserPrincipal up) {
            return up.getUserId();
        }

        // 2) 如果你的自定义用户对象实现了 UserIdAware，直接拿 Long ID
        if (principal instanceof UserIdAware aware) {
            return aware.getId();
        }

        // 3) Spring 自带的 UserDetails：username 若是数字ID则可解析
        if (principal instanceof UserDetails ud) {
            Long parsed = tryParseLong(ud.getUsername());
            if (parsed != null) return parsed;
        }

        // 4) 兜底：auth.getName()（很多情况下是用户名；若是数字ID则解析成功）
        return tryParseLong(auth.getName());
    }

    private Long tryParseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.parseLong(s.trim()); } catch (Exception ignore) { return null; }
    }
}
