package com.xrcgs.iam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xrcgs.common.cache.AuthCacheService;
import com.xrcgs.iam.datascope.DataScopeManager;
import com.xrcgs.iam.entity.SysUser;
import com.xrcgs.iam.enums.DataScope;
import com.xrcgs.iam.mapper.SysUserMapper;
import com.xrcgs.iam.model.dto.UserUpsertDTO;
import com.xrcgs.iam.model.query.UserPageQuery;
import com.xrcgs.iam.model.vo.UserVO;
import com.xrcgs.iam.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final TypeReference<List<Long>> LONG_LIST_TYPE = new TypeReference<>() {
    };

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthCacheService authCacheService;
    private final DataScopeManager dataScopeManager;
    private final ObjectMapper objectMapper;

    @Override
    public Page<UserVO> page(UserPageQuery q, long pageNo, long pageSize) {
        Page<SysUser> entityPage = userMapper.selectPage(new Page<>(pageNo, pageSize), q);
        Page<UserVO> result = new Page<>(entityPage.getCurrent(), entityPage.getSize());
        result.setTotal(entityPage.getTotal());
        result.setPages(entityPage.getPages());
        if (entityPage.getRecords() == null || entityPage.getRecords().isEmpty()) {
            result.setRecords(Collections.emptyList());
            return result;
        }
        result.setRecords(entityPage.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    @Override
    public UserVO detail(Long id) {
        SysUser user = requireExisting(id);
        return toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserUpsertDTO dto) {
        String username = normalize(dto.getUsername());
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        ensureUsernameUnique(username, null);

        String nickname = normalize(dto.getNickname());
        if (!StringUtils.hasText(nickname)) {
            throw new IllegalArgumentException("昵称不能为空");
        }

        String rawPassword = normalize(dto.getPassword());
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("密码不能为空");
        }

        SysUser entity = new SysUser();
        entity.setUsername(username);
        entity.setNickname(nickname);
        entity.setPassword(passwordEncoder.encode(rawPassword));
        entity.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : Boolean.TRUE);
        entity.setDeptId(dto.getDeptId());
        entity.setExtraDeptIds(serializeList(dto.getExtraDeptIds()));

        DataScope dataScope = dto.getDataScope() != null ? dto.getDataScope() : DataScope.SELF;
        entity.setDataScope(dataScope);
        entity.setDataScopeExt(serializeDataScopeExt(dataScope, dto.getDataScopeDeptIds()));

        userMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserUpsertDTO dto) {
        SysUser current = requireExisting(id);

        String username = normalize(dto.getUsername());
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        ensureUsernameUnique(username, id);

        String nickname = normalize(dto.getNickname());
        if (!StringUtils.hasText(nickname)) {
            throw new IllegalArgumentException("昵称不能为空");
        }

        SysUser update = new SysUser();
        update.setId(id);
        update.setUsername(username);
        update.setNickname(nickname);
        update.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : current.getEnabled());
        update.setDeptId(dto.getDeptId());

        String extraDeptJson = dto.getExtraDeptIds() != null ? serializeList(dto.getExtraDeptIds()) : current.getExtraDeptIds();
        update.setExtraDeptIds(extraDeptJson);

        DataScope dataScope = dto.getDataScope() != null ? dto.getDataScope() : current.getDataScope();
        update.setDataScope(dataScope);
        List<Long> scopeDeptIds = dto.getDataScopeDeptIds() != null ? dto.getDataScopeDeptIds() : parseList(current.getDataScopeExt());
        update.setDataScopeExt(serializeDataScopeExt(dataScope, scopeDeptIds));

        String newPassword = normalize(dto.getPassword());
        if (StringUtils.hasText(newPassword)) {
            update.setPassword(passwordEncoder.encode(newPassword));
        }

        userMapper.updateById(update);
        evictAuthCache(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireExisting(id);
        userMapper.deleteById(id);
        evictAuthCache(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEnabled(Long id, boolean enabled) {
        requireExisting(id);
        SysUser update = new SysUser();
        update.setId(id);
        update.setEnabled(enabled);
        userMapper.updateById(update);
        evictAuthCache(id);
    }

    private SysUser requireExisting(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + id);
        }
        return user;
    }

    private void ensureUsernameUnique(String username, Long excludeId) {
        Long count = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .ne(excludeId != null, SysUser::getId, excludeId));
        if (count != null && count > 0) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }
    }

    private String serializeList(List<Long> values) {
        if (values == null) {
            return null;
        }
        List<Long> sanitized = values.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        try {
            return objectMapper.writeValueAsString(sanitized);
        } catch (Exception e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    private List<Long> parseList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, LONG_LIST_TYPE);
        } catch (Exception e) {
            throw new RuntimeException("JSON 解析失败", e);
        }
    }

    private String serializeDataScopeExt(DataScope scope, List<Long> deptIds) {
        if (scope == null || scope != DataScope.CUSTOM) {
            return null;
        }
        return serializeList(deptIds);
    }

    private UserVO toVO(SysUser entity) {
        UserVO vo = new UserVO();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setNickname(entity.getNickname());
        vo.setEnabled(entity.getEnabled());
        vo.setDeptId(entity.getDeptId());
        vo.setExtraDeptIds(parseList(entity.getExtraDeptIds()));
        vo.setDataScope(entity.getDataScope());
        vo.setDataScopeDeptIds(parseList(entity.getDataScopeExt()));
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void evictAuthCache(Long userId) {
        try {
            authCacheService.evictUserPerms(userId);
        } catch (Exception ignored) {
        }
        try {
            dataScopeManager.evictUserDataScope(userId);
        } catch (Exception ignored) {
        }
    }
}

