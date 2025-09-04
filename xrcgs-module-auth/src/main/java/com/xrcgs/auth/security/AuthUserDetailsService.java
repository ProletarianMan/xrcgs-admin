package com.xrcgs.auth.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xrcgs.auth.user.SysUser;
import com.xrcgs.auth.user.SysUserMapper;
import com.xrcgs.iam.entity.SysRole;
import com.xrcgs.iam.entity.SysUserRole;
import com.xrcgs.iam.mapper.SysRoleMapper;
import com.xrcgs.iam.mapper.SysUserRoleMapper;
import com.xrcgs.iam.service.PermService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UserDetailsService 与认证提供者
 * 验证用户信息
 */
@Service
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser u = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username).last("limit 1")
        );

        if (u == null) {
            throw new UsernameNotFoundException("不存在该用户信息！");
        }

        // 获取角色权限
        Set<Long> roleIds = userRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, u.getId()))
                .stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());

        // 2) 角色编码 -> ["ADMIN", "OPS", ...]
        List<String> roleCodes = roleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getCode).toList();

        // 角色/权限：这里先给默认角色，你也可以从表里查出真实角色集合
        List<GrantedAuthority> auths = new ArrayList<>();
        for (String r : roleCodes) {
            auths.add(new SimpleGrantedAuthority("ROLE_" + r));
        }

        boolean enabled = Boolean.TRUE.equals(u.getEnabled());

        // 返回 LoginUser（实现了 UserDetails + UserIdAware）
        LoginUser loginUser = new LoginUser(
                u.getId(),             // ★ 关键：保留用户ID
                u.getUsername(),
                u.getPassword(),       // 已用 BCrypt 存储
                auths
        );

        // 是否启用
        loginUser.setEnabled(enabled);

        return loginUser;
    }

}
