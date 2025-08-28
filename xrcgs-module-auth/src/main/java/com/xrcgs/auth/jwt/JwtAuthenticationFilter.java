package com.xrcgs.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xrcgs.auth.security.AuthUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.*;

import java.io.IOException;
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
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails user = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
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
