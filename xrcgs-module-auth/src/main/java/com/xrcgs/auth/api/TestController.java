package com.xrcgs.auth.api;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xrcgs.auth.user.SysUser;
import com.xrcgs.auth.user.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.xrcgs.syslog.annotation.OpLog;

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

    /**
     * 日志测试接口
     * @param authentication
     * @param boom
     * @return
     */
    @GetMapping("/sysLog")
    @OpLog("查询当前用户")
    public Object sysLog(Authentication authentication,
                     @RequestParam(value = "boom", required = false, defaultValue = "false") boolean boom) {
        if (boom) {
            // 用于演示失败日志（测试后可删掉）
            throw new IllegalArgumentException("演示异常：boom");
        }
        return authentication == null ? null : authentication.getPrincipal();
    }
}
