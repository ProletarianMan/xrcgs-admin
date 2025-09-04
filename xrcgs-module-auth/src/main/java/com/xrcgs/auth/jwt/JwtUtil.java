package com.xrcgs.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties props;

    private Key key;
    private JwtParser parser;

    @PostConstruct
    void init() {
        byte[] keyBytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            // 最少 64 字节，实际应更长/随机；这里不抛错，仅提醒（生产可直接抛异常）
            System.err.println("[警告] JWT 密钥长度 < 64 bytes, 请使用更长的密钥。");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.parser = Jwts.parserBuilder()
                .requireIssuer(props.getIssuer())
                .setSigningKey(key)
                .build();
    }

    public String generateAccessToken(Long id, String username, String nickname, Collection<String> roles, Collection<String> permissions) {
        return buildToken(id, username, nickname, roles, permissions, "access", props.getAccessTtlSeconds());
    }

    public String generateRefreshToken(Long id, String username) {
        return buildToken(id, username, null, null, null, "refresh", props.getRefreshTtlSeconds());
    }

    // 生产token
    private String buildToken(Long id, String username, String nickname, Collection<String> roles, Collection<String> permissions, String typ, long ttlSeconds) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();

        JwtBuilder builder = Jwts.builder()
                .setIssuer(props.getIssuer())
                .setId(jti)
                .setSubject(username)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(ttlSeconds)))
                .claim("typ", typ);

        if (id != null) builder.claim("uid", id);
        if (nickname != null) builder.claim("nickname", nickname);
        if (roles != null) builder.claim("roles", roles);
        if (permissions != null) builder.claim("permissions", permissions);

        return builder
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return parser.parseClaimsJws(token);
    }

    public String getUsername(String token) {
        return parse(token).getBody().getSubject();
    }

    public String getTokenType(String token) {
        Object t = parse(token).getBody().get("typ");
        return t == null ? null : t.toString();
    }

    public String getJti(String token) {
        return parse(token).getBody().getId();
    }

    public Date getExpiration(String token) {
        return parse(token).getBody().getExpiration();
    }

    public boolean isExpired(String token) {
        Date exp = getExpiration(token);
        return exp == null || exp.before(new Date());
    }
}
