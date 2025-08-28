package com.xrcgs.auth.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xrcgs.auth.user.SysUser;
import com.xrcgs.auth.user.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * UserDetailsService 与认证提供者
 * 验证用户信息
 */
@Service
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {

    private final SysUserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser u = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username).last("limit 1")
        );
        if (u == null) {
            throw new UsernameNotFoundException("不存在该用户信息！");
        }
        boolean enabled = Boolean.TRUE.equals(u.getEnabled());
        return User.withUsername(u.getUsername())
                .password(u.getPassword()) // 已用 BCrypt 存储
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))  // 默认带普通用户角色
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!enabled)
                .build();
    }
}
