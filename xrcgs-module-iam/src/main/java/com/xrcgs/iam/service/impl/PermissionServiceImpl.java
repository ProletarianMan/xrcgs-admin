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
                        .orderByAsc(SysPermission::getId)
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

        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("权限编码不能为空");
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("权限名称不能为空");
        }

        ensureCodeUnique(code, null);
        ensureNameUnique(name, null);

        SysPermission entity = new SysPermission();
        entity.setCode(code);
        entity.setName(name);
        permissionMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PermissionUpsertDTO dto) {
        SysPermission current = require(id);

        String code = normalize(dto.getCode());
        String name = normalize(dto.getName());
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("权限编码不能为空");
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("权限名称不能为空");
        }

        ensureCodeUnique(code, id);
        ensureNameUnique(name, id);

        SysPermission update = new SysPermission();
        update.setId(current.getId());
        update.setCode(code);
        update.setName(name);
        permissionMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        require(id);
        permissionMapper.deleteById(id);
        rolePermMapper.delete(
                Wrappers.<SysRolePerm>lambdaQuery().eq(SysRolePerm::getPermId, id)
        );
    }

    private PermissionVO toVO(SysPermission permission) {
        PermissionVO vo = new PermissionVO();
        vo.setId(permission.getId());
        vo.setCode(permission.getCode());
        vo.setName(permission.getName());
        return vo;
    }

    private SysPermission require(Long id) {
        SysPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new IllegalArgumentException("权限不存在");
        }
        return permission;
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
}
