package com.xrcgs.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Map;

/**
 * 权限响应对象
 */
public class AuthDtos {

    @Data
    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
    }

    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class LoginResponse {
        private String username;
        private String nickname;
        private String token;         // access token
        private String refreshToken;
        private long   expiresIn;     // seconds
    }

    @Data
    public static class RefreshRequest {
        @NotBlank private String refreshToken;
    }

    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class TokenResponse {
        private String token;     // access token
        private long   expiresIn; // seconds
        private Map<String, Object> extra; // 扩展可选
    }
}
