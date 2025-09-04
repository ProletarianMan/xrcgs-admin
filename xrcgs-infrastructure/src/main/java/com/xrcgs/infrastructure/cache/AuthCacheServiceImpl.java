package com.xrcgs.infrastructure.cache;

import com.xrcgs.common.cache.AuthCacheService;
import com.xrcgs.common.constants.IamCacheKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.Set;


/**
 * 角色权限调用缓存
 * 权限集合改为 Redis Set 存储，避免拼接分隔符导致的边界问题；支持天然去重。
 * 菜单树/字典仍用 String（JSON）存储，前后端都直观。
 *
 */
@Service
public class AuthCacheServiceImpl implements AuthCacheService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 建议与 JWT 过期时间对齐（示例：8小时）
    private static final Duration PERM_TTL = Duration.ofHours(8);

    // ✅ 新增温和 TTL（可根据你实际调整） 菜单树和字典
    private static final Duration MENU_TREE_TTL = Duration.ofMinutes(10);
    private static final Duration DICT_TTL      = Duration.ofMinutes(30);


    @Override
    public void cacheUserPerms(Long userId, Set<String> perms) {
        String key = IamCacheKeys.AUTH_PERM_USER + userId;
        // 用 Redis Set 存储权限集合，避免逗号拼接问题
        if (perms != null && !perms.isEmpty()) {
            stringRedisTemplate.delete(key);
            stringRedisTemplate.opsForSet().add(key, perms.toArray(String[]::new));
            stringRedisTemplate.expire(key, PERM_TTL);
        }
    }

    @Override
    public Set<String> getCachedUserPerms(Long userId) {
        String key = IamCacheKeys.AUTH_PERM_USER + userId;
        Set<String> members = stringRedisTemplate.opsForSet().members(key);
        return (members == null || members.isEmpty()) ? null : members;
    }

    @Override
    public void evictUserPerms(Long userId) {
        stringRedisTemplate.delete(IamCacheKeys.AUTH_PERM_USER + userId);
    }

    @Override
    public void cacheMenuTreeByRole(Long roleId, String json) {
        stringRedisTemplate.opsForValue()
                .set(IamCacheKeys.MENU_TREE_ROLE + roleId, json, MENU_TREE_TTL);
    }

    @Override
    public String getCachedMenuTreeByRole(Long roleId) {
        return stringRedisTemplate.opsForValue()
                .get(IamCacheKeys.MENU_TREE_ROLE + roleId);
    }

    @Override
    public void evictMenuTreeByRole(Long roleId) {
        stringRedisTemplate.delete(IamCacheKeys.MENU_TREE_ROLE + roleId);
    }

    @Override
    public void cacheDict(String typeCode, String json) {
        stringRedisTemplate.opsForValue()
                .set(IamCacheKeys.DICT_TYPE + typeCode, json, DICT_TTL);
    }

    @Override
    public String getCachedDict(String typeCode) {
        return stringRedisTemplate.opsForValue()
                .get(IamCacheKeys.DICT_TYPE + typeCode);
    }

    @Override
    public void evictDict(String typeCode) {
        stringRedisTemplate.delete(IamCacheKeys.DICT_TYPE + typeCode);
    }
}
