package com.xrcgs.auth.api;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xrcgs.auth.user.SysUser;
import com.xrcgs.auth.user.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 测试与受保护接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/secure")   // 统一前缀
public class TestController {

    private final SysUserMapper userMapper;

    // 受保护接口
    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        String username = authentication.getName();
        SysUser u = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username).last("limit 1"));
        return Map.of(
                "username", username,
                "nickname", u != null ? u.getNickname() : username,
                "authorities", authentication.getAuthorities()
        );
    }
}
