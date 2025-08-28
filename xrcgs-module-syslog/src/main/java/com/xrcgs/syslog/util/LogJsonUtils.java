package com.xrcgs.syslog.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

/**
 * 日志工具类
 */
public final class LogJsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "pwd", "token", "accessToken", "refreshToken", "authorization", "secret"
    );

    private LogJsonUtils() {}

    public static String toJsonSafe(Object obj) {
        if (obj == null) return null;
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }

    public static String maskSensitive(String json) {
        if (json == null) return null;
        String masked = json;
        for (String key : SENSITIVE_KEYS) {
            // 粗暴但有效：将 "key":"xxxx" 里的值替换为 ***
            masked = masked.replaceAll("(\"" + key + "\"\\s*:\\s*\")([^\"]*)(\")", "$1***$3");
        }
        return masked;
    }

    public static boolean isFilteredArg(Object arg) {
        if (arg == null) return true;
        if (arg instanceof HttpServletRequest) return true;
        if (arg instanceof MultipartFile) return true;
        if (arg instanceof InputStream) return true;
        if (arg instanceof OutputStream) return true;
        if (arg.getClass().getName().contains("jakarta.servlet")) return true;
        return false;
    }

    public static String truncate(String str, int maxLen) {
        if (str == null) return null;
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen) + "...(truncated)";
    }

    public static String clientIp(HttpServletRequest request) {
        if (request == null) return null;
        String[] headers = {"X-Forwarded-For","X-Real-IP","Proxy-Client-IP","WL-Proxy-Client-IP"};
        for (String h : headers) {
            String v = request.getHeader(h);
            if (v != null && !v.isBlank() && !"unknown".equalsIgnoreCase(v)) {
                // 可能是 "client, proxy1, proxy2"
                int idx = v.indexOf(',');
                return idx > 0 ? v.substring(0, idx).trim() : v.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
