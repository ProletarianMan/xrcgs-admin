package com.xrcgs.iam.service;

import java.util.Set;

public interface PermService {
    /** 角色集合 -> 权限码集合（菜单.perms + 独立权限码） */
    Set<String> aggregatePermsByRoles(Set<Long> roleIds);

    /** 用户 -> 权限码集合（角色聚合后缓存） */
    Set<String> loadAndCacheUserPerms(Long userId);

    /** 使用户权限缓存失效（角色、菜单、权限变更时调用） */
    void evictUserPerms(Long userId);
}
