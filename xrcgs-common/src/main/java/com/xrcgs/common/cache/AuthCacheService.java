package com.xrcgs.common.cache;

import com.xrcgs.iam.datascope.EffectiveDataScope;

import java.util.Set;

public interface AuthCacheService {
    void cacheUserPerms(Long userId, Set<String> perms);
    Set<String> getCachedUserPerms(Long userId);
    void evictUserPerms(Long userId);

    void cacheUserDataScope(Long userId, EffectiveDataScope scope);
    EffectiveDataScope getCachedUserDataScope(Long userId);
    void evictUserDataScope(Long userId);

    void cacheMenuTreeByRole(Long roleId, String json);
    String getCachedMenuTreeByRole(Long roleId);
    void evictMenuTreeByRole(Long roleId);

    void cacheDict(String typeCode, String json);
    String getCachedDict(String typeCode);
    void evictDict(String typeCode); // 移除缓存
}
