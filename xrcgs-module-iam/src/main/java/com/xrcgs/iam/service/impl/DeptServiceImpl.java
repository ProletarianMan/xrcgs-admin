package com.xrcgs.iam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xrcgs.common.constants.IamCacheKeys;
import com.xrcgs.iam.entity.SysDept;
import com.xrcgs.iam.entity.SysUser;
import com.xrcgs.iam.mapper.SysDeptMapper;
import com.xrcgs.iam.mapper.SysUserMapper;
import com.xrcgs.iam.model.dto.DeptUpsertDTO;
import com.xrcgs.iam.model.vo.DeptTreeVO;
import com.xrcgs.iam.model.vo.DeptVO;
import com.xrcgs.iam.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<DeptTreeVO> tree(String name, Integer status) {
        String keyword = StringUtils.hasText(name) ? name.trim() : null;
        List<SysDept> list = deptMapper.selectList(
                Wrappers.<SysDept>lambdaQuery()
                        .eq(SysDept::getDelFlag, 0)
                        .like(StringUtils.hasText(keyword), SysDept::getName, keyword)
                        .eq(status != null, SysDept::getStatus, status)
                        .orderByAsc(SysDept::getSortNo, SysDept::getId)
        );
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return buildTree(list);
    }

    @Override
    public DeptVO detail(Long id) {
        SysDept dept = requireActive(id);
        return toDetailVO(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DeptUpsertDTO dto) {
        Long parentId = normalizeParentId(dto.getParentId());
        SysDept parent = parentId != null && parentId > 0 ? requireActive(parentId) : null;

        String name = normalize(dto.getName());
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("部门名称不能为空");
        }
        ensureNameUnique(parentId, name, null);

        SysDept entity = new SysDept();
        entity.setParentId(parentId);
        entity.setName(name);
        entity.setCode(normalize(dto.getCode()));
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        entity.setSortNo(dto.getSortNo() != null ? dto.getSortNo() : 0);
        entity.setLeaderUserId(dto.getLeaderUserId());
        entity.setPhone(normalize(dto.getPhone()));
        entity.setEmail(normalize(dto.getEmail()));
        entity.setRemark(normalize(dto.getRemark()));
        entity.setPath(buildPendingPath(parentId));

        deptMapper.insert(entity);

        String newPath = buildPath(parent, entity.getId());
        entity.setPath(newPath);
        deptMapper.updateById(entity);

        bumpTreeVersionAndEvict(Collections.singletonList(entity.getId()));
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DeptUpsertDTO dto) {
        SysDept current = requireActive(id);
        Long parentId = normalizeParentId(dto.getParentId());
        SysDept parent = parentId != null && parentId > 0 ? requireActive(parentId) : null;

        if (Objects.equals(id, parentId)) {
            throw new IllegalArgumentException("父级部门不能是自身");
        }
        if (parent != null && parent.getPath() != null && current.getPath() != null
                && parent.getPath().startsWith(current.getPath())) {
            throw new IllegalArgumentException("不能将部门移动到自己的子部门下");
        }

        String name = normalize(dto.getName());
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("部门名称不能为空");
        }
        ensureNameUnique(parentId, name, id);

        SysDept update = new SysDept();
        update.setId(id);
        update.setParentId(parentId);
        update.setName(name);
        update.setCode(normalize(dto.getCode()));
        update.setStatus(dto.getStatus() != null ? dto.getStatus() : current.getStatus());
        update.setSortNo(dto.getSortNo() != null ? dto.getSortNo() : current.getSortNo());
        update.setLeaderUserId(dto.getLeaderUserId());
        update.setPhone(normalize(dto.getPhone()));
        update.setEmail(normalize(dto.getEmail()));
        update.setRemark(normalize(dto.getRemark()));

        String newPath = buildPath(parent, id);
        boolean pathChanged = !Objects.equals(newPath, current.getPath());
        update.setPath(newPath);

        List<Long> affectedIds = deptMapper.selectIdsByPathPrefix(current.getPath());
        if (affectedIds == null || affectedIds.isEmpty()) {
            affectedIds = Collections.singletonList(id);
        }
        if (pathChanged) {
            deptMapper.updatePathPrefix(current.getPath(), newPath);
        }
        deptMapper.updateById(update);

        bumpTreeVersionAndEvict(affectedIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysDept dept = requireActive(id);
        Long childCount = deptMapper.countChildren(id);
        if (childCount != null && childCount > 0) {
            throw new IllegalStateException("存在子部门，无法删除");
        }
        List<Long> affectedIds = deptMapper.selectIdsByPathPrefix(dept.getPath());
        if (affectedIds == null || affectedIds.isEmpty()) {
            affectedIds = Collections.singletonList(dept.getId());
        }
        deptMapper.deleteById(id);
        bumpTreeVersionAndEvict(affectedIds);
    }

    /* ----------------- helpers ----------------- */

    private List<DeptTreeVO> buildTree(List<SysDept> flat) {
        Set<Long> leaderIds = flat.stream()
                .map(SysDept::getLeaderUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DeptTreeVO.LeaderUser> leaderUsers = Collections.emptyMap();
        if (!leaderIds.isEmpty()) {
            List<SysUser> users = userMapper.selectBatchIds(leaderIds);
            if (users != null && !users.isEmpty()) {
                Map<Long, DeptTreeVO.LeaderUser> temp = new HashMap<>(users.size());
                for (SysUser user : users) {
                    if (user == null || user.getId() == null) {
                        continue;
                    }
                    DeptTreeVO.LeaderUser leaderUser = new DeptTreeVO.LeaderUser();
                    leaderUser.setId(user.getId());
                    String nickname = user.getNickname();
                    String username = user.getUsername();
                    leaderUser.setName(StringUtils.hasText(nickname) ? nickname : username);
                    temp.put(user.getId(), leaderUser);
                }
                leaderUsers = temp;
            }
        }
        Map<Long, DeptTreeVO> map = new HashMap<>(flat.size());
        for (SysDept dept : flat) {
            DeptTreeVO node = new DeptTreeVO();
            node.setId(dept.getId());
            node.setParentId(dept.getParentId());
            node.setName(dept.getName());
            node.setCode(dept.getCode());
            node.setStatus(dept.getStatus());
            node.setSortNo(dept.getSortNo());
            node.setPath(dept.getPath());
            Long leaderUserId = dept.getLeaderUserId();
            node.setLeaderUser(leaderUserId == null ? null : leaderUsers.get(leaderUserId));
            node.setPhone(dept.getPhone());
            node.setEmail(dept.getEmail());
            node.setRemark(dept.getRemark());
            node.setCreateTime(dept.getCreateTime());
            map.put(dept.getId(), node);
        }

        List<DeptTreeVO> roots = new ArrayList<>();
        for (SysDept dept : flat) {
            DeptTreeVO node = map.get(dept.getId());
            Long pid = dept.getParentId();
            if (pid == null || pid == 0 || !map.containsKey(pid)) {
                roots.add(node);
            } else {
                map.get(pid).getChildren().add(node);
            }
        }
        return roots;
    }

    private DeptVO toDetailVO(SysDept dept) {
        DeptVO vo = new DeptVO();
        vo.setId(dept.getId());
        vo.setParentId(dept.getParentId());
        vo.setPath(dept.getPath());
        vo.setName(dept.getName());
        vo.setCode(dept.getCode());
        vo.setStatus(dept.getStatus());
        vo.setSortNo(dept.getSortNo());
        vo.setLeaderUserId(dept.getLeaderUserId());
        vo.setPhone(dept.getPhone());
        vo.setEmail(dept.getEmail());
        vo.setRemark(dept.getRemark());
        vo.setCreateBy(dept.getCreateBy());
        vo.setCreateTime(dept.getCreateTime());
        vo.setUpdateBy(dept.getUpdateBy());
        vo.setUpdateTime(dept.getUpdateTime());
        return vo;
    }

    private SysDept requireActive(Long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null || (dept.getDelFlag() != null && dept.getDelFlag() == 1)) {
            throw new IllegalArgumentException("部门不存在: " + id);
        }
        return dept;
    }

    private void ensureNameUnique(Long parentId, String name, Long excludeId) {
        Long count = deptMapper.selectCount(
                Wrappers.<SysDept>lambdaQuery()
                        .eq(SysDept::getParentId, parentId)
                        .eq(SysDept::getName, name)
                        .eq(SysDept::getDelFlag, 0)
                        .ne(excludeId != null, SysDept::getId, excludeId)
        );
        if (count != null && count > 0) {
            throw new IllegalArgumentException("同一父级下部门名称已存在");
        }
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildPendingPath(Long parentId) {
        long random = ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L);
        long pid = parentId == null ? 0L : parentId;
        return "/" + pid + "/" + random + "/";
    }

    private String buildPath(SysDept parent, Long selfId) {
        String prefix = parent == null ? "/" : parent.getPath();
        if (prefix == null || prefix.isEmpty()) {
            prefix = "/";
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        return prefix + selfId + "/";
    }

    private void bumpTreeVersionAndEvict(Collection<Long> deptIds) {
        try {
            stringRedisTemplate.opsForValue().increment(IamCacheKeys.DEPT_TREE_VERSION);
        } catch (Exception ignored) {
        }
        if (deptIds == null || deptIds.isEmpty()) {
            return;
        }
        List<String> keys = deptIds.stream()
                .filter(Objects::nonNull)
                .map(id -> IamCacheKeys.DEPT_SCOPE + id)
                .collect(Collectors.toList());
        if (keys.isEmpty()) {
            return;
        }
        try {
            stringRedisTemplate.delete(keys);
        } catch (Exception ignored) {
        }
    }
}
