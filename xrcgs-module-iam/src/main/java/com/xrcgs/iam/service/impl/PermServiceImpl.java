package com.xrcgs.iam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xrcgs.iam.datascope.DataScopeManager;
import com.xrcgs.iam.entity.SysMenu;
import com.xrcgs.iam.entity.SysPermission;
import com.xrcgs.iam.entity.SysRolePerm;
import com.xrcgs.iam.entity.SysUserRole;
import com.xrcgs.iam.mapper.SysMenuMapper;
import com.xrcgs.iam.mapper.SysPermissionMapper;
import com.xrcgs.iam.mapper.SysRolePermMapper;
import com.xrcgs.iam.mapper.SysUserRoleMapper;
import com.xrcgs.iam.service.PermService;
import com.xrcgs.common.cache.AuthCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermServiceImpl implements PermService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRolePermMapper rolePermMapper;
    private final SysPermissionMapper permissionMapper;
    private final AuthCacheService cache;
    private final DataScopeManager dataScopeManager;

    @Override
    public Set<String> aggregatePermsByRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return Collections.emptySet();

        // 1) 角色 -> 菜单 perms
        Set<String> permsFromMenu = new HashSet<>();
        for (Long rid : roleIds) {
            List<SysMenu> ms = menuMapper.selectByRoleId(rid);
            for (SysMenu m : ms) {
                if (m.getStatus() != null && m.getStatus() == 1 && m.getDelFlag() != null && m.getDelFlag() == 0) {
                    if (m.getPerms() != null && !m.getPerms().isBlank()) {
                        permsFromMenu.add(m.getPerms().trim());
                    }
                }
            }
        }

        // 2) 角色 -> 独立权限码
        Set<String> permsFromRolePerm = new HashSet<>();
        List<SysRolePerm> rps = rolePermMapper.selectList(
                Wrappers.<SysRolePerm>lambdaQuery().in(SysRolePerm::getRoleId, roleIds));
        if (!rps.isEmpty()) {
            Set<Long> permIds = rps.stream().map(SysRolePerm::getPermId).collect(Collectors.toSet());
            if (!permIds.isEmpty()) {
                List<SysPermission> ps = permissionMapper.selectBatchIds(permIds);
                for (SysPermission p : ps) {
                    if (p.getCode() != null && !p.getCode().isBlank()) permsFromRolePerm.add(p.getCode().trim());
                }
            }
        }
        permsFromMenu.addAll(permsFromRolePerm);
        return permsFromMenu;
    }

    @Override
    public Set<String> loadAndCacheUserPerms(Long userId) {
        // 先尝试读缓存（infrastructure 用 Redis Set 存）
        Set<String> cached = cache.getCachedUserPerms(userId);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // 用户 -> 角色
        List<SysUserRole> urs = userRoleMapper.selectList(
                Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        Set<Long> roleIds = urs.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());

        Set<String> perms = aggregatePermsByRoles(roleIds);
        cache.cacheUserPerms(userId, perms);
        return perms;
    }

    @Override
    public void evictUserPerms(Long userId) {
        cache.evictUserPerms(userId);
        dataScopeManager.evictUserDataScope(userId);
    }
}
