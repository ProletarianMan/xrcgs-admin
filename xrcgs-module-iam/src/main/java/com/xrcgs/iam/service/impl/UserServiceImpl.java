package com.xrcgs.iam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xrcgs.common.cache.AuthCacheService;
import com.xrcgs.iam.datascope.DataScopeManager;
import com.xrcgs.iam.entity.SysDept;
import com.xrcgs.iam.entity.SysRole;
import com.xrcgs.iam.entity.SysUser;
import com.xrcgs.iam.entity.SysUserRole;
import com.xrcgs.iam.enums.DataScope;
import com.xrcgs.iam.mapper.SysDeptMapper;
import com.xrcgs.iam.mapper.SysUserMapper;
import com.xrcgs.iam.mapper.SysUserRoleMapper;
import com.xrcgs.iam.mapper.SysRoleMapper;
import com.xrcgs.iam.model.dto.UserUpsertDTO;
import com.xrcgs.iam.model.query.UserPageQuery;
import com.xrcgs.iam.model.vo.DeptBriefVO;
import com.xrcgs.iam.model.vo.RoleBriefVO;
import com.xrcgs.iam.model.vo.UserVO;
import com.xrcgs.iam.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final TypeReference<List<Long>> LONG_LIST_TYPE = new TypeReference<>() {
    };

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthCacheService authCacheService;
    private final DataScopeManager dataScopeManager;
    private final ObjectMapper objectMapper;
    private final SysUserRoleMapper userRoleMapper;

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
        List<UserVO> records = entityPage.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        fillDeptInfo(records);
        fillRoleInfo(records);
        result.setRecords(records);
        return result;
    }

    @Override
    public UserVO detail(Long id) {
        SysUser user = requireExisting(id);
        UserVO vo = toVO(user);
        fillDeptInfo(Collections.singletonList(vo));
        fillRoleInfo(Collections.singletonList(vo));
        return vo;
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
        entity.setWechatId(normalizeAllowEmpty(dto.getWechatId()));
        entity.setPhone(normalizeAllowEmpty(dto.getPhone()));
        entity.setGender(dto.getGender());
        entity.setPassword(passwordEncoder.encode(rawPassword));
        entity.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : Boolean.TRUE);
        entity.setDeptId(dto.getDeptId());
        entity.setExtraDeptIds(serializeList(dto.getExtraDeptIds()));

        DataScope dataScope = dto.getDataScope() != null ? dto.getDataScope() : DataScope.SELF;
        entity.setDataScope(dataScope);
        entity.setDataScopeExt(serializeDataScopeExt(dataScope, dto.getDataScopeDeptIds()));

        userMapper.insert(entity);
        saveUserRoles(entity.getId(), dto.getRoleIds());
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
        update.setWechatId(dto.getWechatId() != null ? normalizeAllowEmpty(dto.getWechatId()) : current.getWechatId());
        update.setPhone(dto.getPhone() != null ? normalizeAllowEmpty(dto.getPhone()) : current.getPhone());
        update.setGender(dto.getGender() != null ? dto.getGender() : current.getGender());
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
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
        saveUserRoles(id, dto.getRoleIds());
        evictAuthCache(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String rawPassword) {
        requireExisting(id);
        String normalized = normalize(rawPassword);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("密码不能为空");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(normalized));
        userMapper.updateById(update);
        evictAuthCache(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long id, List<Long> roleIds) {
        requireExisting(id);
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
        saveUserRoles(id, roleIds);
        evictAuthCache(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireExisting(id);
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
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

    @Override
    public List<UserVO> listByNicknameSuffix(String nickname) {
        String normalized = normalize(nickname);
        if (!StringUtils.hasText(normalized)) {
            return Collections.emptyList();
        }
        List<SysUser> users = userMapper.selectByNicknameSuffix(normalized);
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserVO> vos = users.stream().map(this::toVO).collect(Collectors.toList());
        fillDeptInfo(vos);
        fillRoleInfo(vos);
        return vos;
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

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (userId == null || roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<SysUserRole> relations = roleIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(roleId -> {
                    SysUserRole relation = new SysUserRole();
                    relation.setUserId(userId);
                    relation.setRoleId(roleId);
                    return relation;
                })
                .collect(Collectors.toList());
        if (relations.isEmpty()) {
            return;
        }
        relations.forEach(userRoleMapper::insert);
    }

    private UserVO toVO(SysUser entity) {
        UserVO vo = new UserVO();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setNickname(entity.getNickname());
        vo.setWechatId(entity.getWechatId());
        vo.setPhone(entity.getPhone());
        vo.setGender(entity.getGender());
        vo.setEnabled(entity.getEnabled());
        if (entity.getDeptId() != null) {
            DeptBriefVO dept = new DeptBriefVO();
            dept.setId(entity.getDeptId());
            vo.setDept(dept);
        }
        vo.setExtraDeptIds(parseList(entity.getExtraDeptIds()));
        vo.setDataScope(entity.getDataScope());
        vo.setDataScopeDeptIds(parseList(entity.getDataScopeExt()));
        vo.setRoles(Collections.emptyList());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private void fillRoleInfo(List<UserVO> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<Long> userIds = users.stream()
                .map(UserVO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }
        List<SysUserRole> relations = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                .in(SysUserRole::getUserId, userIds));
        if (relations == null || relations.isEmpty()) {
            return;
        }
        Map<Long, List<Long>> userRoleIdMap = new HashMap<>();
        List<Long> roleIds = new ArrayList<>();
        for (SysUserRole relation : relations) {
            if (relation.getUserId() == null || relation.getRoleId() == null) {
                continue;
            }
            userRoleIdMap.computeIfAbsent(relation.getUserId(), k -> new ArrayList<>())
                    .add(relation.getRoleId());
            roleIds.add(relation.getRoleId());
        }
        if (roleIds.isEmpty()) {
            return;
        }
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds.stream().distinct().collect(Collectors.toList()));
        if (roles == null || roles.isEmpty()) {
            return;
        }
        Map<Long, SysRole> roleMap = roles.stream()
                .filter(role -> role.getId() != null)
                .collect(Collectors.toMap(SysRole::getId, role -> role, (a, b) -> a));
        users.forEach(user -> {
            List<Long> mappedRoleIds = userRoleIdMap.get(user.getId());
            if (mappedRoleIds == null || mappedRoleIds.isEmpty()) {
                user.setRoles(Collections.emptyList());
                return;
            }
            List<RoleBriefVO> roleVos = mappedRoleIds.stream()
                    .distinct()
                    .map(roleMap::get)
                    .filter(Objects::nonNull)
                    .map(role -> {
                        RoleBriefVO roleVo = new RoleBriefVO();
                        roleVo.setId(role.getId());
                        roleVo.setName(role.getName());
                        return roleVo;
                    })
                    .collect(Collectors.toList());
            user.setRoles(roleVos);
        });
    }

    private void fillDeptInfo(List<UserVO> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<Long> deptIds = users.stream()
                .map(UserVO::getDept)
                .filter(Objects::nonNull)
                .map(DeptBriefVO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (deptIds.isEmpty()) {
            return;
        }
        Map<Long, SysDept> deptMap = loadDeptMap(deptIds);
        users.stream()
                .map(UserVO::getDept)
                .filter(Objects::nonNull)
                .forEach(dept -> {
                    SysDept entity = deptMap.get(dept.getId());
                    if (entity != null) {
                        dept.setName(entity.getName());
                    }
                });
    }

    private Map<Long, SysDept> loadDeptMap(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysDept> depts = deptMapper.selectBatchIds(deptIds);
        if (depts == null || depts.isEmpty()) {
            return Collections.emptyMap();
        }
        return depts.stream().collect(Collectors.toMap(SysDept::getId, dept -> dept, (a, b) -> a));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeAllowEmpty(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
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

