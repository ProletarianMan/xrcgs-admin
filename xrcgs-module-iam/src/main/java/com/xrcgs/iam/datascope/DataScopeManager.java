package com.xrcgs.iam.datascope;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xrcgs.common.cache.AuthCacheService;
import com.xrcgs.common.constants.IamCacheKeys;
import com.xrcgs.iam.entity.SysDept;
import com.xrcgs.iam.entity.SysRole;
import com.xrcgs.iam.entity.SysUser;
import com.xrcgs.iam.entity.SysUserRole;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DataScopeManager {

    private final AuthCacheService authCacheService;
    private final DataScopeCalculator calculator;
    private final com.xrcgs.iam.mapper.SysUserMapper userMapper;
    private final com.xrcgs.iam.mapper.SysUserRoleMapper userRoleMapper;
    private final com.xrcgs.iam.mapper.SysRoleMapper roleMapper;
    private final com.xrcgs.iam.mapper.SysDeptMapper deptMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public DataScopeManager(AuthCacheService authCacheService,
                            DataScopeCalculator calculator,
                            com.xrcgs.iam.mapper.SysUserMapper userMapper,
                            com.xrcgs.iam.mapper.SysUserRoleMapper userRoleMapper,
                            com.xrcgs.iam.mapper.SysRoleMapper roleMapper,
                            com.xrcgs.iam.mapper.SysDeptMapper deptMapper,
                            StringRedisTemplate stringRedisTemplate) {
        this.authCacheService = authCacheService;
        this.calculator = calculator;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.deptMapper = deptMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public EffectiveDataScope getEffectiveDataScope(Long userId) {
        if (userId == null) {
            return EffectiveDataScope.selfOnly();
        }
        long currentVersion = currentDeptTreeVersion();
        EffectiveDataScope cached = authCacheService.getCachedUserDataScope(userId);
        if (cached != null) {
            long cachedVersion = DataScopeUtil.nullSafeVersion(cached.getDeptTreeVersion());
            if (cachedVersion == currentVersion) {
                return cached;
            }
        }
        EffectiveDataScope computed = compute(userId);
        computed.setDeptTreeVersion(currentVersion);
        authCacheService.cacheUserDataScope(userId, computed);
        return computed;
    }

    public void evictUserDataScope(Long userId) {
        if (userId == null) {
            return;
        }
        authCacheService.evictUserDataScope(userId);
    }

    private EffectiveDataScope compute(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return EffectiveDataScope.selfOnly();
        }
        List<SysUserRole> relations = userRoleMapper.selectList(
                Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId)
        );
        Set<Long> roleIds = relations.stream()
                .map(SysUserRole::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<SysRole> roles;
        if (roleIds.isEmpty()) {
            roles = Collections.emptyList();
        } else {
            roles = roleMapper.selectBatchIds(roleIds);
        }
        if (roles != null) {
            roles = roles.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            roles = Collections.emptyList();
        }
        List<SysDept> depts = deptMapper.selectList(
                Wrappers.<SysDept>lambdaQuery().eq(SysDept::getDelFlag, 0)
        );
        if (depts == null) {
            depts = Collections.emptyList();
        }
        return calculator.calculate(user, roles, depts);
    }

    private long currentDeptTreeVersion() {
        try {
            ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
            if (ops == null) {
                return 0L;
            }
            String value = ops.get(IamCacheKeys.DEPT_TREE_VERSION);
            if (value == null || value.isBlank()) {
                return 0L;
            }
            return Long.parseLong(value.trim());
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
