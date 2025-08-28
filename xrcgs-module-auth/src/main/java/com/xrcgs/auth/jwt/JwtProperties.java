package com.xrcgs.auth.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * jwt相关属性
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private String issuer = "xrcgs";
    private long accessTtlSeconds = 3600;   // 1h
    private long refreshTtlSeconds = 604800; // 7d
    private String header = "Authorization";
    private String prefix = "Bearer ";
}
