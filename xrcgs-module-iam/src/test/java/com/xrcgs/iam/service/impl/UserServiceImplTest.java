package com.xrcgs.iam.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xrcgs.common.cache.AuthCacheService;
import com.xrcgs.iam.datascope.DataScopeManager;
import com.xrcgs.iam.entity.SysDept;
import com.xrcgs.iam.entity.SysUser;
import com.xrcgs.iam.enums.DataScope;
import com.xrcgs.iam.mapper.SysDeptMapper;
import com.xrcgs.iam.mapper.SysUserMapper;
import com.xrcgs.iam.model.dto.UserUpsertDTO;
import com.xrcgs.iam.model.query.UserPageQuery;
import com.xrcgs.iam.model.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SysDeptMapper sysDeptMapper;

    @Mock
    private AuthCacheService authCacheService;

    @Mock
    private DataScopeManager dataScopeManager;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userMapper, sysDeptMapper, passwordEncoder, authCacheService, dataScopeManager, new ObjectMapper());
    }

    @Test
    void createShouldEncodePasswordAndPersist() {
        UserUpsertDTO dto = new UserUpsertDTO();
        dto.setUsername(" admin ");
        dto.setPassword("plainPwd");
        dto.setNickname(" 管理员 ");
        dto.setWechatId(" wechat_001 ");
        dto.setPhone(" 13800138000 ");
        dto.setGender(1);
        dto.setEnabled(true);
        dto.setDeptId(1L);
        dto.setExtraDeptIds(Arrays.asList(10L, 20L));
        dto.setDataScope(DataScope.CUSTOM);
        dto.setDataScopeDeptIds(Arrays.asList(100L, 200L));

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("plainPwd")).thenReturn("encodedPwd");
        doAnswer(invocation -> {
            SysUser entity = invocation.getArgument(0);
            entity.setId(5L);
            return 1;
        }).when(userMapper).insert(any(SysUser.class));

        Long id = userService.create(dto);
        assertEquals(5L, id);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(captor.capture());
        SysUser saved = captor.getValue();
        assertEquals("admin", saved.getUsername());
        assertEquals("管理员", saved.getNickname());
        assertEquals("wechat_001", saved.getWechatId());
        assertEquals("13800138000", saved.getPhone());
        assertEquals(1, saved.getGender());
        assertEquals("encodedPwd", saved.getPassword());
        assertEquals(Boolean.TRUE, saved.getEnabled());
        assertEquals(1L, saved.getDeptId());
        assertEquals("[10,20]", saved.getExtraDeptIds());
        assertEquals(DataScope.CUSTOM, saved.getDataScope());
        assertEquals("[100,200]", saved.getDataScopeExt());
    }

    @Test
    void createShouldRejectDuplicateUsername() {
        UserUpsertDTO dto = new UserUpsertDTO();
        dto.setUsername("admin");
        dto.setPassword("plainPwd");
        dto.setNickname("管理员");

        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () -> userService.create(dto));
        verify(userMapper, never()).insert(any());
    }

    @Test
    void updateShouldEncodePasswordWhenProvided() {
        SysUser current = new SysUser();
        current.setId(10L);
        current.setUsername("old");
        current.setNickname("旧");
        current.setWechatId("oldWechat");
        current.setPhone("000111");
        current.setGender(1);
        current.setEnabled(Boolean.TRUE);
        current.setDeptId(2L);
        current.setExtraDeptIds("[1]");
        current.setDataScope(DataScope.SELF);
        current.setDataScopeExt(null);

        when(userMapper.selectById(10L)).thenReturn(current);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("newPwd")).thenReturn("encodedNew");

        UserUpsertDTO dto = new UserUpsertDTO();
        dto.setUsername(" new ");
        dto.setNickname(" 新 ");
        dto.setPassword("newPwd");
        dto.setWechatId(" newWechat ");
        dto.setPhone(" 111222333 ");
        dto.setGender(0);
        dto.setEnabled(false);
        dto.setDeptId(3L);
        dto.setExtraDeptIds(Arrays.asList(5L, 6L));
        dto.setDataScope(DataScope.CUSTOM);
        dto.setDataScopeDeptIds(Arrays.asList(9L, 10L));

        userService.update(10L, dto);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).updateById(captor.capture());
        SysUser updated = captor.getValue();
        assertEquals(10L, updated.getId());
        assertEquals("new", updated.getUsername());
        assertEquals("新", updated.getNickname());
        assertEquals("newWechat", updated.getWechatId());
        assertEquals("111222333", updated.getPhone());
        assertEquals(0, updated.getGender());
        assertEquals(Boolean.FALSE, updated.getEnabled());
        assertEquals(3L, updated.getDeptId());
        assertEquals("[5,6]", updated.getExtraDeptIds());
        assertEquals(DataScope.CUSTOM, updated.getDataScope());
        assertEquals("[9,10]", updated.getDataScopeExt());
        assertEquals("encodedNew", updated.getPassword());

        verify(authCacheService).evictUserPerms(10L);
        verify(dataScopeManager).evictUserDataScope(10L);
    }

    @Test
    void updateShouldKeepPasswordAndJsonWhenMissing() {
        SysUser current = new SysUser();
        current.setId(11L);
        current.setUsername("old");
        current.setNickname("旧");
        current.setWechatId("keepWechat");
        current.setPhone("keepPhone");
        current.setGender(1);
        current.setEnabled(Boolean.TRUE);
        current.setDeptId(4L);
        current.setExtraDeptIds("[3]");
        current.setDataScope(DataScope.CUSTOM);
        current.setDataScopeExt("[7]");

        when(userMapper.selectById(11L)).thenReturn(current);
        when(userMapper.selectCount(any())).thenReturn(0L);

        UserUpsertDTO dto = new UserUpsertDTO();
        dto.setUsername("newer");
        dto.setNickname("更新");
        dto.setPassword(null);
        dto.setEnabled(null);
        dto.setDeptId(5L);
        dto.setExtraDeptIds(null);
        dto.setDataScope(null);
        dto.setDataScopeDeptIds(null);

        userService.update(11L, dto);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).updateById(captor.capture());
        SysUser updated = captor.getValue();
        assertEquals(11L, updated.getId());
        assertNull(updated.getPassword());
        assertEquals("keepWechat", updated.getWechatId());
        assertEquals("keepPhone", updated.getPhone());
        assertEquals(1, updated.getGender());
        assertEquals("[3]", updated.getExtraDeptIds());
        assertEquals(DataScope.CUSTOM, updated.getDataScope());
        assertEquals("[7]", updated.getDataScopeExt());
        assertEquals(Boolean.TRUE, updated.getEnabled());

        verify(passwordEncoder, never()).encode(any());
        verify(authCacheService).evictUserPerms(11L);
        verify(dataScopeManager).evictUserDataScope(11L);
    }

    @Test
    void updateEnabledShouldToggleStatus() {
        SysUser current = new SysUser();
        current.setId(15L);
        current.setEnabled(Boolean.TRUE);

        when(userMapper.selectById(15L)).thenReturn(current);

        userService.updateEnabled(15L, false);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).updateById(captor.capture());
        SysUser updated = captor.getValue();
        assertEquals(15L, updated.getId());
        assertEquals(Boolean.FALSE, updated.getEnabled());

        verify(authCacheService).evictUserPerms(15L);
        verify(dataScopeManager).evictUserDataScope(15L);
    }

    @Test
    void deleteShouldRemoveUserAndEvictCaches() {
        SysUser current = new SysUser();
        current.setId(20L);

        when(userMapper.selectById(20L)).thenReturn(current);

        userService.delete(20L);

        verify(userMapper).deleteById(20L);
        verify(authCacheService).evictUserPerms(20L);
        verify(dataScopeManager).evictUserDataScope(20L);
    }

    @Test
    void pageShouldConvertEntitiesToVo() {
        SysUser record = new SysUser();
        record.setId(1L);
        record.setUsername("alice");
        record.setNickname("Alice");
        record.setWechatId("wechatAlice");
        record.setPhone("18800001111");
        record.setGender(0);
        record.setEnabled(Boolean.TRUE);
        record.setDeptId(9L);
        record.setExtraDeptIds("[30,40]");
        record.setDataScope(DataScope.CUSTOM);
        record.setDataScopeExt("[50]");
        record.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        record.setUpdatedAt(LocalDateTime.of(2024, 1, 2, 10, 0));

        Page<SysUser> mpPage = new Page<>(1, 10);
        mpPage.setTotal(1);
        mpPage.setPages(1);
        mpPage.setRecords(Collections.singletonList(record));

        when(userMapper.selectPage(any(Page.class), any())).thenReturn(mpPage);
        SysDept dept = new SysDept();
        dept.setId(9L);
        dept.setName("研发部");
        when(sysDeptMapper.selectBatchIds(List.of(9L))).thenReturn(List.of(dept));

        UserPageQuery query = new UserPageQuery();
        query.setUsername("alice");

        Page<UserVO> result = userService.page(query, 1, 10);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());
        assertEquals(1, result.getRecords().size());
        UserVO vo = result.getRecords().get(0);
        assertEquals("alice", vo.getUsername());
        assertEquals("Alice", vo.getNickname());
        assertEquals("wechatAlice", vo.getWechatId());
        assertEquals("18800001111", vo.getPhone());
        assertEquals(0, vo.getGender());
        assertEquals(Boolean.TRUE, vo.getEnabled());
        assertNotNull(vo.getDept());
        assertEquals(9L, vo.getDept().getId());
        assertEquals("研发部", vo.getDept().getName());
        assertEquals(List.of(30L, 40L), vo.getExtraDeptIds());
        assertEquals(DataScope.CUSTOM, vo.getDataScope());
        assertEquals(List.of(50L), vo.getDataScopeDeptIds());
        assertEquals(record.getCreatedAt(), vo.getCreatedAt());
        assertEquals(record.getUpdatedAt(), vo.getUpdatedAt());
    }

    @Test
    void detailShouldReturnVo() {
        SysUser record = new SysUser();
        record.setId(8L);
        record.setUsername("bob");
        record.setNickname("Bob");
        record.setWechatId("wechatBob");
        record.setPhone("19900002222");
        record.setGender(1);
        record.setEnabled(Boolean.FALSE);
        record.setDeptId(12L);
        record.setExtraDeptIds("[1,2]");
        record.setDataScope(DataScope.SELF);
        record.setDataScopeExt(null);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());

        when(userMapper.selectById(8L)).thenReturn(record);
        SysDept dept = new SysDept();
        dept.setId(12L);
        dept.setName("财务部");
        when(sysDeptMapper.selectBatchIds(List.of(12L))).thenReturn(List.of(dept));

        UserVO vo = userService.detail(8L);
        assertEquals("bob", vo.getUsername());
        assertEquals(Boolean.FALSE, vo.getEnabled());
        assertEquals("wechatBob", vo.getWechatId());
        assertEquals("19900002222", vo.getPhone());
        assertEquals(1, vo.getGender());
        assertNotNull(vo.getDept());
        assertEquals(12L, vo.getDept().getId());
        assertEquals("财务部", vo.getDept().getName());
        assertEquals(List.of(1L, 2L), vo.getExtraDeptIds());
        assertEquals(Collections.emptyList(), vo.getDataScopeDeptIds());
    }

    @Test
    void detailShouldThrowWhenNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> userService.detail(999L));
    }

    @Test
    void listByNicknameSuffixShouldReturnConvertedUsers() {
        SysUser user = new SysUser();
        user.setId(30L);
        user.setUsername("nickuser");
        user.setNickname("Nick");
        user.setWechatId("wechatNick");
        user.setPhone("17700003333");
        user.setGender(0);
        user.setEnabled(Boolean.TRUE);
        user.setDeptId(7L);
        user.setExtraDeptIds("[5,6]");
        user.setDataScope(DataScope.CUSTOM);
        user.setDataScopeExt("[101,102]");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        when(userMapper.selectByNicknameSuffix("Nick")).thenReturn(List.of(user));
        SysDept dept = new SysDept();
        dept.setId(7L);
        dept.setName("市场部");
        when(sysDeptMapper.selectBatchIds(List.of(7L))).thenReturn(List.of(dept));

        List<UserVO> result = userService.listByNicknameSuffix(" Nick ");

        assertNotNull(result);
        assertEquals(1, result.size());
        UserVO vo = result.get(0);
        assertEquals(user.getId(), vo.getId());
        assertEquals(user.getUsername(), vo.getUsername());
        assertEquals(user.getNickname(), vo.getNickname());
        assertEquals(user.getWechatId(), vo.getWechatId());
        assertEquals(user.getPhone(), vo.getPhone());
        assertEquals(user.getGender(), vo.getGender());
        assertEquals(user.getEnabled(), vo.getEnabled());
        assertNotNull(vo.getDept());
        assertEquals(user.getDeptId(), vo.getDept().getId());
        assertEquals("市场部", vo.getDept().getName());
        assertEquals(List.of(5L, 6L), vo.getExtraDeptIds());
        assertEquals(DataScope.CUSTOM, vo.getDataScope());
        assertEquals(List.of(101L, 102L), vo.getDataScopeDeptIds());
        verify(userMapper).selectByNicknameSuffix("Nick");
    }

    @Test
    void listByNicknameSuffixShouldReturnEmptyWhenNicknameBlank() {
        List<UserVO> result = userService.listByNicknameSuffix("   ");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userMapper, never()).selectByNicknameSuffix(any());
    }
}

