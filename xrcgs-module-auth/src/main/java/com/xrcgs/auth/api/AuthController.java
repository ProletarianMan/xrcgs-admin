package com.xrcgs.auth.api;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xrcgs.auth.api.dto.AuthDtos.*;
import com.xrcgs.auth.jwt.*;
import com.xrcgs.auth.user.SysUser;
import com.xrcgs.auth.user.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JwtProperties props;
    private final TokenBlacklistService blacklistService;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder; // 仅供调试/导入使用，可删

    /** 登入 */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );
        UserDetails principal = (UserDetails) auth.getPrincipal();

        SysUser db = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, principal.getUsername()).last("limit 1"));

        String nickname = db != null ? db.getNickname() : principal.getUsername();

        String access = jwtUtil.generateAccessToken(principal.getUsername(), nickname, List.of("ROLE_USER"));
        String refresh = jwtUtil.generateRefreshToken(principal.getUsername());

        return ResponseEntity.ok(LoginResponse.builder()
                .username(principal.getUsername())
                .nickname(nickname)
                .token(access)
                .refreshToken(refresh)
                .expiresIn(props.getAccessTtlSeconds())
                .build());
    }

    /** 登出：将当前 Access Token（以及可选的 Refresh Token）列入黑名单 */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    @RequestBody(required = false) RefreshRequest body) {
        // 1) 动态读取你配置的 Header 名称（默认 Authorization）
        String authHeader = request.getHeader(props.getHeader());

        // 2) 拉黑 Access Token（来自 Authorization）
        if (authHeader != null && authHeader.startsWith(props.getPrefix())) {
            String access = authHeader.substring(props.getPrefix().length()).trim();
            blacklistService.blacklistToken(access, jwtUtil);
        }

        // 3) 拉黑 Refresh Token（如传）
        if (body != null && body.getRefreshToken() != null) {
            blacklistService.blacklistToken(body.getRefreshToken(), jwtUtil);
        }

        return ResponseEntity.ok().build();
    }

    /** 刷新：用合法且未拉黑的 Refresh Token 换新 Access Token */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        String refreshToken = req.getRefreshToken();

        // 验证 refresh token
        if (jwtUtil.isExpired(refreshToken)) {
            return ResponseEntity.status(401).build();
        }
        String typ = jwtUtil.getTokenType(refreshToken);
        if (!"refresh".equals(typ)) {
            return ResponseEntity.status(401).build();
        }
        if (blacklistService.isBlacklisted(jwtUtil.getJti(refreshToken))) {
            return ResponseEntity.status(401).build();
        }

        String username = jwtUtil.getUsername(refreshToken);
        SysUser db = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username).last("limit 1"));
        if (db == null || !Boolean.TRUE.equals(db.getEnabled())) {
            return ResponseEntity.status(401).build();
        }

        String newAccess = jwtUtil.generateAccessToken(username, db.getNickname(), List.of("ROLE_USER"));
        return ResponseEntity.ok(TokenResponse.builder()
                .token(newAccess)
                .expiresIn(props.getAccessTtlSeconds())
                .build());
    }
}
