package com.xrcgs.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xrcgs.auth.security.AuthUserDetailsService;
import com.xrcgs.common.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * JWT 过滤器（OncePerRequestFilter）
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtProperties props;
    private final TokenBlacklistService blacklistService;
    private final AuthUserDetailsService userDetailsService;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(props.getHeader());
        if (authHeader == null || !authHeader.startsWith(props.getPrefix())) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(props.getPrefix().length()).trim();
        try {
            // 解析 & 校验基础信息
            Jws<Claims> jws = jwtUtil.parse(token);
            Claims claims = jws.getBody();

            // 仅接受 access token
            String typ = String.valueOf(claims.get("typ"));
            if (!"access".equals(typ)) {
                unauthorized(response, "无效令牌类型！");
                return;
            }

            String jti = claims.getId();
            if (blacklistService.isBlacklisted(jti)) {
                unauthorized(response, "令牌已被拉黑！");
                return;
            }

            String username = claims.getSubject();
            Long userId = claims.get("uid", Long.class);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//                UserDetails user = userDetailsService.loadUserByUsername(username);

                // === 变更点 #2：不再查库，直接把 roles/perms 从 claims 注入到 authorities ===
                List<GrantedAuthority> auths = new ArrayList<>();

                // 角色权限
                // roles: ["ADMIN","OPS"] -> ROLE_ADMIN / ROLE_OPS
                List<String> roles = claims.get("roles", List.class);
                if (roles != null) {
                    for (String r : roles) {
                        auths.add(new SimpleGrantedAuthority("ROLE_" + r));
                    }
                }

                // 按钮权限
                // perms: ["iam:user:list", ...] -> PERM_iam:user:list
                Collection<String> perms = claims.get("permissions", Collection.class);
                if (perms != null) {
                    for (String p : perms) {
                        auths.add(new SimpleGrantedAuthority("PERM_" + p));
                    }
                }

                // principal：给 hasPerm() 的表达式 Root 提取 userId 做兜底回源（必要时）
                UserPrincipal principal = new UserPrincipal(userId, username);

//                UsernamePasswordAuthenticationToken authentication =
//                        new UsernamePasswordAuthenticationToken(user, null, auths);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, auths);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            unauthorized(response, "令牌已过期");
        } catch (JwtException | IllegalArgumentException e) {
            unauthorized(response, "无效令牌");
        }

//        chain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        MAPPER.writeValue(response.getWriter(), Map.of("code", 401, "error", "UNAUTHORIZED", "message", message));
    }
}
