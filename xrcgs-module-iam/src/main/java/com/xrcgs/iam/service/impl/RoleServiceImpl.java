package com.xrcgs.iam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xrcgs.common.cache.AuthCacheService;
import com.xrcgs.iam.datascope.DataScopeManager;
import com.xrcgs.iam.entity.*;
import com.xrcgs.iam.mapper.*;
import com.xrcgs.iam.model.dto.RoleGrantMenuDTO;
import com.xrcgs.iam.model.dto.RoleGrantPermDTO;
import com.xrcgs.iam.model.dto.RoleUpsertDTO;
import com.xrcgs.iam.model.query.RolePageQuery;
import com.xrcgs.iam.model.vo.DeptBriefVO;
import com.xrcgs.iam.model.vo.RolePageVO;
import com.xrcgs.iam.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRolePermMapper rolePermMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysDeptMapper deptMapper;
    private final AuthCacheService authCacheService;
    private final DataScopeManager dataScopeManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long upsert(RoleUpsertDTO dto) {
        // 唯一性校验
        Long exists = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getCode, dto.getCode())
                .ne(dto.getId() != null, SysRole::getId, dto.getId()));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("角色编码已存在: " + dto.getCode());
        }

        SysRole origin = null;
        if (dto.getId() != null) {
            origin = roleMapper.selectById(dto.getId());
            if (origin == null) {
                throw new IllegalArgumentException("角色不存在: " + dto.getId());
            }
        }

        SysRole role = new SysRole();
        role.setId(dto.getId());
        role.setCode(dto.getCode());
        role.setName(dto.getName());
        Integer status = dto.getStatus();
        if (status == null) {
            status = origin != null ? origin.getStatus() : 1;
        }
        role.setStatus(status);
        role.setDeptId(dto.getDeptId() == null && origin != null ? origin.getDeptId() : dto.getDeptId());

        Integer sortNo = dto.getSortNo();
        if (sortNo == null) {
            if (dto.getId() == null) {
                Integer maxSortNo = roleMapper.selectMaxSortNo();
                sortNo = (maxSortNo == null ? 1 : maxSortNo + 1);
            } else {
                sortNo = origin.getSortNo();
            }
        }
        role.setSortNo(sortNo);
        role.setDataScope(dto.getDataScope() == null && origin != null ? origin.getDataScope() : dto.getDataScope());
        try {
            if (role.getDataScope() != null && "CUSTOM".equals(role.getDataScope().name())) {
                if (dto.getDataScopeDeptIds() == null && origin != null && origin.getDataScope() == role.getDataScope()) {
                    role.setDataScopeExt(origin.getDataScopeExt());
                } else {
                    role.setDataScopeExt(objectMapper.writeValueAsString(dto.getDataScopeDeptIds()));
                }
            } else {
                role.setDataScopeExt(null);
            }
        } catch (Exception e) {
            throw new RuntimeException("dataScopeExt 序列化失败", e);
        }
        role.setRemark(dto.getRemark() == null && origin != null ? origin.getRemark() : dto.getRemark());

        if (role.getId() == null) roleMapper.insert(role);
        else roleMapper.updateById(role);

        // 角色被变更 -> 使拥有该角色的用户权限缓存失效
        invalidateUsersByRole(role.getId());
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long roleId) {
        roleMapper.deleteById(roleId);
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, roleId));
        rolePermMapper.delete(Wrappers.<SysRolePerm>lambdaQuery().eq(SysRolePerm::getRoleId, roleId));
        invalidateUsersByRole(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantMenus(RoleGrantMenuDTO dto) {
        Long roleId = dto.getRoleId();
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, roleId));
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            for (Long mid : dto.getMenuIds()) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(mid);
                roleMenuMapper.insert(rm);
            }
        }
        // ✅ 精确失效：该角色的菜单树 + 该角色下所有用户的权限缓存
        authCacheService.evictMenuTreeByRole(roleId);

        invalidateUsersByRole(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantPerms(RoleGrantPermDTO dto) {
        Long roleId = dto.getRoleId();
        rolePermMapper.delete(Wrappers.<SysRolePerm>lambdaQuery().eq(SysRolePerm::getRoleId, roleId));
        if (dto.getPermIds() != null && !dto.getPermIds().isEmpty()) {
            for (Long pid : dto.getPermIds()) {
                SysRolePerm rp = new SysRolePerm();
                rp.setRoleId(roleId);
                rp.setPermId(pid);
                rolePermMapper.insert(rp);
            }
        }
        invalidateUsersByRole(roleId);
    }

    @Override
    public Page<RolePageVO> page(RolePageQuery q, long pageNo, long pageSize) {
        Page<SysRole> rawPage = roleMapper.selectPage(new Page<>(pageNo, pageSize), q);
        List<SysRole> records = rawPage.getRecords();
        if (records == null || records.isEmpty()) {
            Page<RolePageVO> empty = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
            empty.setRecords(Collections.emptyList());
            empty.setPages(rawPage.getPages());
            return empty;
        }

        Set<Long> deptIds = new HashSet<>();
        for (SysRole role : records) {
            if (role.getDeptId() != null) {
                deptIds.add(role.getDeptId());
            }
        }

        Map<Long, SysDept> deptMap = Collections.emptyMap();
        if (!deptIds.isEmpty()) {
            List<SysDept> depts = deptMapper.selectBatchIds(deptIds);
            if (depts != null && !depts.isEmpty()) {
                deptMap = new HashMap<>(depts.size());
                for (SysDept dept : depts) {
                    deptMap.put(dept.getId(), dept);
                }
            }
        }

        List<RolePageVO> vos = new ArrayList<>(records.size());
        for (SysRole role : records) {
            RolePageVO vo = new RolePageVO();
            BeanUtils.copyProperties(role, vo);
            Long deptId = role.getDeptId();
            if (deptId != null) {
                SysDept dept = deptMap.get(deptId);
                if (dept != null) {
                    DeptBriefVO deptBriefVO = new DeptBriefVO();
                    deptBriefVO.setId(dept.getId());
                    deptBriefVO.setName(dept.getName());
                    vo.setDept(deptBriefVO);
                }
            }
            vos.add(vo);
        }

        Page<RolePageVO> result = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        result.setPages(rawPage.getPages());
        result.setRecords(vos);
        return result;
    }

    @Override
    public List<Long> listMenuIdsByRole(Long roleId) {
        List<SysRoleMenu> list = roleMenuMapper.selectList(
                Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, roleId));
        List<Long> ids = new ArrayList<>();
        for (SysRoleMenu rm : list) ids.add(rm.getMenuId());
        return ids;
    }

    @Override
    public List<String> listPermIdsByRole(Long roleId) {
        List<SysRolePerm> list = rolePermMapper.selectList(
                Wrappers.<SysRolePerm>lambdaQuery().eq(SysRolePerm::getRoleId, roleId));
        List<String> ids = new ArrayList<>();
        for (SysRolePerm rp : list) {
            Long pid = rp.getPermId();
            if (pid != null) {
                ids.add(String.valueOf(pid));
            }
        }
        return ids;
    }

    private void invalidateUsersByRole(Long roleId) {
        List<SysUserRole> urs = userRoleMapper.selectList(
                Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getRoleId, roleId));
        for (SysUserRole ur : urs) {
            authCacheService.evictUserPerms(ur.getUserId());
            dataScopeManager.evictUserDataScope(ur.getUserId());
        }
    }
}
