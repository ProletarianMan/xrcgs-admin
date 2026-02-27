package com.xrcgs.iam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xrcgs.iam.entity.SysPermission;
import com.xrcgs.iam.entity.SysRolePerm;
import com.xrcgs.iam.mapper.SysPermissionMapper;
import com.xrcgs.iam.mapper.SysRolePermMapper;
import com.xrcgs.iam.model.dto.PermissionUpsertDTO;
import com.xrcgs.iam.model.vo.PermissionVO;
import com.xrcgs.iam.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final SysPermissionMapper permissionMapper;
    private final SysRolePermMapper rolePermMapper;

    @Override
    public List<PermissionVO> list(String name) {
        String keyword = null;
        boolean prefix = false;
        if (StringUtils.hasText(name)) {
            String normalized = name.trim();
            if (normalized.endsWith("*")) {
                prefix = true;
                normalized = normalized.substring(0, normalized.length() - 1).trim();
            }
            keyword = StringUtils.hasText(normalized) ? normalized : null;
        }

        List<SysPermission> permissions = permissionMapper.selectList(
                Wrappers.<SysPermission>lambdaQuery()
                        .likeRight(prefix && keyword != null, SysPermission::getName, keyword)
                        .eq(!prefix && keyword != null, SysPermission::getName, keyword)
                        .orderByAsc(SysPermission::getSortNo, SysPermission::getId)
        );
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }
        return permissions.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PermissionUpsertDTO dto) {
        String code = normalize(dto.getCode());
        String name = normalize(dto.getName());
        String remark = normalize(dto.getRemark());
        Long parentId = normalizeParentId(dto.getParentId());
        Integer sortNo = normalizeSortNo(dto.getOrder());

        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("权限编码不能为空");
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("权限名称不能为空");
        }

        ensureCodeUnique(code, null);
        ensureNameUnique(name, null);
        validateParent(parentId, null);

        SysPermission entity = new SysPermission();
        entity.setParentId(parentId);
        entity.setCode(code);
        entity.setName(name);
        entity.setRemark(remark);
        entity.setSortNo(sortNo);
        permissionMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PermissionUpsertDTO dto) {
        SysPermission current = require(id);

        String code = normalize(dto.getCode());
        String name = normalize(dto.getName());
        String remark = normalize(dto.getRemark());
        Long parentId = normalizeParentId(dto.getParentId());
        Integer sortNo = normalizeSortNo(dto.getOrder());

        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("权限编码不能为空");
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("权限名称不能为空");
        }

        if (Objects.equals(id, parentId)) {
            throw new IllegalArgumentException("父权限不能是自身");
        }

        ensureCodeUnique(code, id);
        ensureNameUnique(name, id);
        validateParent(parentId, id);

        SysPermission update = new SysPermission();
        update.setId(current.getId());
        update.setParentId(parentId);
        update.setCode(code);
        update.setName(name);
        update.setRemark(remark);
        update.setSortNo(sortNo);
        permissionMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<Long> targetIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (targetIds.isEmpty()) {
            return;
        }

        List<SysPermission> existing = permissionMapper.selectBatchIds(targetIds);
        if (existing == null || existing.size() != targetIds.size()) {
            throw new IllegalArgumentException("部分权限不存在或已删除");
        }

        List<Long> deleteIds = expandDeleteIds(existing, targetIds);
        permissionMapper.deleteBatchIds(deleteIds);
        rolePermMapper.delete(
                Wrappers.<SysRolePerm>lambdaQuery().in(SysRolePerm::getPermId, deleteIds)
        );
    }

    private PermissionVO toVO(SysPermission permission) {
        PermissionVO vo = new PermissionVO();
        vo.setId(permission.getId());
        vo.setParentId(permission.getParentId() == null ? 0L : permission.getParentId());
        vo.setCode(permission.getCode());
        vo.setName(permission.getName());
        vo.setRemark(permission.getRemark());
        vo.setOrder(permission.getSortNo());
        return vo;
    }

    private SysPermission require(Long id) {
        SysPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new IllegalArgumentException("权限不存在");
        }
        return permission;
    }

    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (selfId != null && Objects.equals(parentId, selfId)) {
            throw new IllegalArgumentException("父权限不能是自身");
        }
        SysPermission parent = permissionMapper.selectById(parentId);
        if (parent == null) {
            throw new IllegalArgumentException("父权限不存在");
        }
    }

    private void ensureCodeUnique(String code, Long excludeId) {
        Long count = permissionMapper.selectCount(
                Wrappers.<SysPermission>lambdaQuery()
                        .eq(SysPermission::getCode, code)
                        .ne(excludeId != null, SysPermission::getId, excludeId)
        );
        if (count != null && count > 0) {
            throw new IllegalStateException("权限编码已存在");
        }
    }

    private void ensureNameUnique(String name, Long excludeId) {
        Long count = permissionMapper.selectCount(
                Wrappers.<SysPermission>lambdaQuery()
                        .eq(SysPermission::getName, name)
                        .ne(excludeId != null, SysPermission::getId, excludeId)
        );
        if (count != null && count > 0) {
            throw new IllegalStateException("权限名称已存在");
        }
    }

    private String normalize(String text) {
        return text == null ? null : text.trim();
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private Integer normalizeSortNo(Integer order) {
        return order == null ? 0 : order;
    }

    private List<Long> expandDeleteIds(List<SysPermission> existing, List<Long> targetIds) {
        if (existing == null || existing.isEmpty()) {
            return targetIds;
        }
        boolean hasRoot = existing.stream().anyMatch(p -> p != null && isRoot(p.getParentId()));
        if (!hasRoot) {
            return targetIds;
        }

        List<SysPermission> all = permissionMapper.selectList(
                Wrappers.<SysPermission>lambdaQuery()
                        .select(SysPermission::getId, SysPermission::getParentId)
        );
        if (all == null || all.isEmpty()) {
            return targetIds;
        }

        // collect children of any root permissions in targetIds (including deeper descendants)
        List<Long> rootIds = existing.stream()
                .filter(p -> p != null && isRoot(p.getParentId()))
                .map(SysPermission::getId)
                .collect(Collectors.toList());

        List<Long> deleteIds = targetIds.stream().distinct().collect(Collectors.toList());
        java.util.Set<Long> deleteSet = new java.util.LinkedHashSet<>(deleteIds);
        boolean added;
        do {
            added = false;
            for (SysPermission p : all) {
                if (p == null || p.getParentId() == null) {
                    continue;
                }
                if (deleteSet.contains(p.getParentId()) && !deleteSet.contains(p.getId())) {
                    deleteSet.add(p.getId());
                    added = true;
                }
            }
        } while (added);

        // ensure rootIds are included
        for (Long rootId : rootIds) {
            deleteSet.add(rootId);
        }
        return new java.util.ArrayList<>(deleteSet);
    }

    private boolean isRoot(Long parentId) {
        return parentId == null || parentId == 0L;
    }
}
