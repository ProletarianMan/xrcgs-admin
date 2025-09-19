package com.xrcgs.iam.datascope;

import com.xrcgs.common.cache.AuthCacheService;
import com.xrcgs.common.constants.IamCacheKeys;
import com.xrcgs.iam.entity.SysDept;
import com.xrcgs.iam.entity.SysRole;
import com.xrcgs.iam.entity.SysUser;
import com.xrcgs.iam.entity.SysUserRole;
import com.xrcgs.iam.enums.DataScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataScopeManagerTest {

    @Mock
    private AuthCacheService authCacheService;
    @Mock
    private com.xrcgs.iam.mapper.SysUserMapper userMapper;
    @Mock
    private com.xrcgs.iam.mapper.SysUserRoleMapper userRoleMapper;
    @Mock
    private com.xrcgs.iam.mapper.SysRoleMapper roleMapper;
    @Mock
    private com.xrcgs.iam.mapper.SysDeptMapper deptMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private DataScopeManager manager;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        manager = new DataScopeManager(authCacheService, new DataScopeCalculator(),
                userMapper, userRoleMapper, roleMapper, deptMapper, stringRedisTemplate);
    }

    @Test
    void shouldReturnCachedScopeWhenVersionMatches() {
        when(valueOperations.get(IamCacheKeys.DEPT_TREE_VERSION)).thenReturn("5");
        EffectiveDataScope cached = EffectiveDataScope.ofDepartments(Set.of(10L), true);
        cached.setDeptTreeVersion(5L);
        when(authCacheService.getCachedUserDataScope(1L)).thenReturn(cached);

        EffectiveDataScope result = manager.getEffectiveDataScope(1L);

        assertSame(cached, result);
        verifyNoInteractions(userMapper, userRoleMapper, roleMapper, deptMapper);
        verify(authCacheService, never()).cacheUserDataScope(anyLong(), any());
    }

    @Test
    void shouldRecalculateWhenVersionMismatch() {
        when(valueOperations.get(IamCacheKeys.DEPT_TREE_VERSION)).thenReturn("7");
        EffectiveDataScope cached = EffectiveDataScope.selfOnly();
        cached.setDeptTreeVersion(2L);
        when(authCacheService.getCachedUserDataScope(1L)).thenReturn(cached);

        SysUser user = new SysUser();
        user.setId(1L);
        user.setDeptId(10L);
        user.setDataScope(DataScope.DEPT);
        when(userMapper.selectById(1L)).thenReturn(user);

        SysUserRole relation = new SysUserRole();
        relation.setRoleId(100L);
        when(userRoleMapper.selectList(any())).thenReturn(List.of(relation));

        SysRole role = new SysRole();
        role.setId(100L);
        role.setStatus(1);
        role.setDelFlag(0);
        role.setDataScope(DataScope.CUSTOM);
        role.setDataScopeExt("[11]");
        when(roleMapper.selectBatchIds(anyCollection())).thenReturn(List.of(role));

        SysDept dept10 = new SysDept();
        dept10.setId(10L);
        dept10.setParentId(0L);
        dept10.setDelFlag(0);
        SysDept dept11 = new SysDept();
        dept11.setId(11L);
        dept11.setParentId(10L);
        dept11.setDelFlag(0);
        when(deptMapper.selectList(any())).thenReturn(List.of(dept10, dept11));

        EffectiveDataScope result = manager.getEffectiveDataScope(1L);

        assertFalse(result.isAll());
        assertTrue(result.getDeptIds().contains(11L));
        assertEquals(7L, DataScopeUtil.nullSafeVersion(result.getDeptTreeVersion()));

        verify(authCacheService).cacheUserDataScope(eq(1L), argThat(scope ->
                scope.getDeptIds().contains(11L)
                        && DataScopeUtil.nullSafeVersion(scope.getDeptTreeVersion()) == 7L));
    }
}
