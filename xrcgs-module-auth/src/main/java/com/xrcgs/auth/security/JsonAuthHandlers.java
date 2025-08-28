package com.xrcgs.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.*;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 认证异常处理器（JSON 输出）
 */
@Component
public class JsonAuthHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException authException) throws IOException {
        write(response, 401, "UNAUTHORIZED", authException.getMessage());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        write(response, 403, "FORBIDDEN", accessDeniedException.getMessage());
    }

    private void write(HttpServletResponse resp, int code, String error, String msg) throws IOException {
        resp.setStatus(code);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(resp.getWriter(), Map.of(
                "code", code,
                "error", error,
                "message", msg
        ));
    }
}
