package com.xrcgs.iam.service.impl;

import com.xrcgs.common.constants.IamCacheKeys;
import com.xrcgs.iam.entity.SysDept;
import com.xrcgs.iam.entity.SysUser;
import com.xrcgs.iam.mapper.SysDeptMapper;
import com.xrcgs.iam.mapper.SysUserMapper;
import com.xrcgs.iam.model.dto.DeptUpsertDTO;
import com.xrcgs.iam.model.vo.DeptTreeVO;
import com.xrcgs.iam.model.vo.DeptVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeptServiceImplTest {

    @Mock
    private SysDeptMapper deptMapper;

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private DeptServiceImpl deptService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.delete(ArgumentMatchers.<Collection<String>>any())).thenReturn(1L);
        deptService = new DeptServiceImpl(deptMapper, userMapper, redisTemplate);
    }

    @Test
    void createShouldInsertAndUpdatePathAndBumpCache() {
        DeptUpsertDTO dto = new DeptUpsertDTO();
        dto.setParentId(1L);
        dto.setName("研发中心");
        dto.setSortNo(10);

        SysDept parent = new SysDept();
        parent.setId(1L);
        parent.setPath("/1/");

        when(deptMapper.selectById(1L)).thenReturn(parent);
        when(deptMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            SysDept arg = invocation.getArgument(0);
            arg.setId(10L);
            return 1;
        }).when(deptMapper).insert(any(SysDept.class));

        Long id = deptService.create(dto);
        assertEquals(10L, id);

        ArgumentCaptor<SysDept> updateCaptor = ArgumentCaptor.forClass(SysDept.class);
        verify(deptMapper).updateById(updateCaptor.capture());
        SysDept updated = updateCaptor.getValue();
        assertEquals("/1/10/", updated.getPath());
        assertEquals("研发中心", updated.getName());
        assertEquals(1L, updated.getParentId());

        verify(valueOperations).increment(IamCacheKeys.DEPT_TREE_VERSION);
        verify(redisTemplate).delete(ArgumentMatchers.<Collection<String>>argThat(keys -> keys.contains(IamCacheKeys.DEPT_SCOPE + "10")));
    }

    @Test
    void updateShouldMoveDepartmentAndEvictCaches() {
        DeptUpsertDTO dto = new DeptUpsertDTO();
        dto.setParentId(3L);
        dto.setName("新名称");
        dto.setSortNo(20);

        SysDept current = new SysDept();
        current.setId(2L);
        current.setParentId(1L);
        current.setPath("/1/2/");
        current.setStatus(1);
        current.setSortNo(10);

        SysDept newParent = new SysDept();
        newParent.setId(3L);
        newParent.setPath("/1/3/");

        when(deptMapper.selectById(2L)).thenReturn(current);
        when(deptMapper.selectById(3L)).thenReturn(newParent);
        when(deptMapper.selectCount(any())).thenReturn(0L);
        when(deptMapper.selectIdsByPathPrefix("/1/2/")).thenReturn(Arrays.asList(2L, 5L));

        deptService.update(2L, dto);

        verify(deptMapper).updatePathPrefix("/1/2/", "/1/3/2/");

        ArgumentCaptor<SysDept> updateCaptor = ArgumentCaptor.forClass(SysDept.class);
        verify(deptMapper).updateById(updateCaptor.capture());
        SysDept updated = updateCaptor.getValue();
        assertEquals("/1/3/2/", updated.getPath());
        assertEquals(3L, updated.getParentId());
        assertEquals("新名称", updated.getName());

        verify(valueOperations, atLeastOnce()).increment(IamCacheKeys.DEPT_TREE_VERSION);
        verify(redisTemplate).delete(ArgumentMatchers.<Collection<String>>argThat(keys ->
                keys.contains(IamCacheKeys.DEPT_SCOPE + "2") &&
                        keys.contains(IamCacheKeys.DEPT_SCOPE + "5")));
    }

    @Test
    void updateShouldRejectCycle() {
        SysDept current = new SysDept();
        current.setId(2L);
        current.setPath("/1/2/");

        SysDept parent = new SysDept();
        parent.setId(5L);
        parent.setPath("/1/2/5/");

        when(deptMapper.selectById(2L)).thenReturn(current);
        when(deptMapper.selectById(5L)).thenReturn(parent);

        DeptUpsertDTO dto = new DeptUpsertDTO();
        dto.setParentId(5L);
        dto.setName("测试");

        assertThrows(IllegalArgumentException.class, () -> deptService.update(2L, dto));
    }

    @Test
    void deleteShouldFailWhenChildrenExist() {
        SysDept current = new SysDept();
        current.setId(2L);
        current.setPath("/1/2/");

        when(deptMapper.selectById(2L)).thenReturn(current);
        when(deptMapper.countChildren(2L)).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> deptService.delete(2L));
        verify(deptMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteShouldEvictCaches() {
        SysDept current = new SysDept();
        current.setId(4L);
        current.setPath("/1/4/");

        when(deptMapper.selectById(4L)).thenReturn(current);
        when(deptMapper.countChildren(4L)).thenReturn(0L);
        when(deptMapper.selectIdsByPathPrefix("/1/4/")).thenReturn(Collections.singletonList(4L));

        deptService.delete(4L);

        verify(deptMapper).deleteById(4L);
        verify(redisTemplate).delete(ArgumentMatchers.<Collection<String>>argThat(keys -> keys.contains(IamCacheKeys.DEPT_SCOPE + "4")));
    }

    @Test
    void treeShouldBuildHierarchy() {
        SysDept root = new SysDept();
        root.setId(1L);
        root.setParentId(0L);
        root.setName("根");
        root.setSortNo(1);

        SysDept child = new SysDept();
        child.setId(2L);
        child.setParentId(1L);
        child.setName("子");
        child.setSortNo(2);

        root.setLeaderUserId(100L);
        child.setLeaderUserId(101L);

        when(deptMapper.selectList(any())).thenReturn(Arrays.asList(root, child));

        SysUser rootLeader = new SysUser();
        rootLeader.setId(100L);
        rootLeader.setNickname("张三");
        SysUser childLeader = new SysUser();
        childLeader.setId(101L);
        childLeader.setNickname("李四");
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(Arrays.asList(rootLeader, childLeader));

        List<DeptTreeVO> tree = deptService.tree(null, null);
        assertEquals(1, tree.size());
        DeptTreeVO rootNode = tree.get(0);
        assertEquals("根", rootNode.getName());
        assertEquals(1, rootNode.getChildren().size());
        assertEquals("子", rootNode.getChildren().get(0).getName());
        assertNotNull(rootNode.getLeaderUser());
        assertEquals(100L, rootNode.getLeaderUser().getId());
        assertEquals("张三", rootNode.getLeaderUser().getName());
        DeptTreeVO childNode = rootNode.getChildren().get(0);
        assertNotNull(childNode.getLeaderUser());
        assertEquals(101L, childNode.getLeaderUser().getId());
        assertEquals("李四", childNode.getLeaderUser().getName());
    }

    @Test
    void detailShouldReturnDeptVo() {
        SysDept dept = new SysDept();
        dept.setId(3L);
        dept.setName("市场");

        when(deptMapper.selectById(3L)).thenReturn(dept);

        DeptVO vo = deptService.detail(3L);
        assertEquals("市场", vo.getName());
    }
}
