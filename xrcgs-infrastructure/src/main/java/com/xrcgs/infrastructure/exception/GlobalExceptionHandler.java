package com.xrcgs.infrastructure.exception;

import com.xrcgs.common.event.SystemErrorEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：发布 SystemErrorEvent，避免二次抛出。
 * 这里不用 Lombok，避免 IDE 未开启注解处理导致“变量未初始化”。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ApplicationEventPublisher publisher;

    @Autowired
    public GlobalExceptionHandler(ApplicationEventPublisher publisher) {
        this.publisher = publisher; // <- 这样就不会提示“might not have been initialized”
    }

    @ExceptionHandler(Exception.class)
    public Object handleAll(Exception ex, HttpServletRequest request) {
        String httpMethod = safe(() -> request.getMethod());
        String uri = safe(() -> request.getRequestURI());
        String query = safe(() -> request.getQueryString());
        String ip = clientIp(request);

        String username = currentUsername();

        String exMsg = ex.getClass().getSimpleName() + ": " + (ex.getMessage() == null ? "" : ex.getMessage());
        if (exMsg.length() > 1000) exMsg = exMsg.substring(0, 1000) + "...(truncated)";

        // 发布事件（由 syslog 模块的监听器入库）
        publisher.publishEvent(new SystemErrorEvent(this, "系统异常", username,
                httpMethod, uri, ip, query, exMsg));

        // 这里返回你项目统一的错误响应结构（示例）
        return new ErrorResponse(500, "INTERNAL_ERROR", "服务器错误");
    }

    // —— 工具方法 —— //
    private static String clientIp(HttpServletRequest request) {
        if (request == null) return null;
        String[] headers = { "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP" };
        for (String h : headers) {
            String v = request.getHeader(h);
            if (v != null && !v.isBlank() && !"unknown".equalsIgnoreCase(v)) {
                int idx = v.indexOf(',');
                return idx > 0 ? v.substring(0, idx).trim() : v.trim();
            }
        }
        return request.getRemoteAddr();
    }

    private static String currentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return (auth != null) ? auth.getName() : "anonymous";
        } catch (Exception ignored) {
            return "anonymous";
        }
    }

    private static <T> T safe(SupplierEx<T> s) {
        try { return s.get(); } catch (Exception e) { return null; }
    }
    private interface SupplierEx<T> { T get() throws Exception; }

    // —— 你的统一错误返回体（示例，可替换为现有类） —— //
    private record ErrorResponse(int code, String error, String message) {}
}
