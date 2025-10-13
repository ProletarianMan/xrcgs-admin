package com.xrcgs.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xrcgs.common.cache.AuthCacheService;
import com.xrcgs.iam.datascope.DataScopeManager;
import com.xrcgs.iam.datascope.EffectiveDataScope;
import com.xrcgs.iam.entity.SysDept;
import com.xrcgs.iam.entity.SysDictItem;
import com.xrcgs.iam.entity.SysDictType;
import com.xrcgs.iam.entity.SysUser;
import com.xrcgs.iam.mapper.SysDeptMapper;
import com.xrcgs.iam.mapper.SysDictItemMapper;
import com.xrcgs.iam.mapper.SysDictTypeMapper;
import com.xrcgs.iam.mapper.SysUserMapper;
import com.xrcgs.iam.model.query.DictItemPageQuery;
import com.xrcgs.iam.model.query.DictTypePageQuery;
import com.xrcgs.iam.model.vo.DeptBriefVO;
import com.xrcgs.iam.model.vo.DictVO;
import com.xrcgs.iam.service.DictService;
import com.xrcgs.infrastructure.audit.UserIdProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DictServiceImpl implements DictService {

    private final SysDictTypeMapper typeMapper;
    private final SysDictItemMapper itemMapper;
    private final SysDeptMapper deptMapper;
    private final AuthCacheService cache;
    private final DataScopeManager dataScopeManager;
    private final UserIdProvider userIdProvider;
    private final SysUserMapper userMapper;
    private final ObjectMapper om = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createType(SysDictType type) {
        Long c = typeMapper.selectCount(Wrappers.<SysDictType>lambdaQuery().eq(SysDictType::getCode, type.getCode()));
        if (c != null && c > 0) throw new IllegalArgumentException("字典类型已存在: " + type.getCode());
        if (type.getStatus() == null) type.setStatus(1);
        typeMapper.insert(type);
        syncTypeCache(type.getCode());
        return type.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateType(SysDictType type) {
        SysDictType origin = type.getId() == null ? null : typeMapper.selectById(type.getId());
        typeMapper.updateById(type);
        String newCode = StringUtils.hasText(type.getCode())
                ? type.getCode()
                : origin != null ? origin.getCode() : null;
        if (origin != null && StringUtils.hasText(origin.getCode())
                && !Objects.equals(origin.getCode(), newCode)) {
            cache.evictDict(origin.getCode());
        }
        syncTypeCache(newCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeType(Long id) {
        SysDictType t = typeMapper.selectById(id);
        if (t == null) return;
        itemMapper.delete(Wrappers.<SysDictItem>lambdaQuery().eq(SysDictItem::getTypeCode, t.getCode()));
        typeMapper.deleteById(id);
        cache.evictDict(t.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createItem(SysDictItem item) {
        if (item.getStatus() == null) item.setStatus(1);
        // 未显式指定部门时，默认归属当前操作人的部门，保证数据范围受控
        if (item.getDeptId() == null) {
            Long currentUserId = userIdProvider.getCurrentUserId();
            item.setDeptId(resolveDeptId(currentUserId));
        }
        itemMapper.insert(item);
        syncTypeCache(item.getTypeCode());
        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(SysDictItem item) {
        SysDictItem origin = item.getId() == null ? null : itemMapper.selectById(item.getId());
        itemMapper.updateById(item);
        if (origin != null && StringUtils.hasText(origin.getTypeCode())
                && !Objects.equals(origin.getTypeCode(), item.getTypeCode())) {
            syncTypeCache(origin.getTypeCode());
        }
        syncTypeCache(item.getTypeCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long id) {
        SysDictItem item = itemMapper.selectById(id);
        if (item != null) {
            itemMapper.deleteById(id);
            syncTypeCache(item.getTypeCode());
        }
    }

    @Override
    public DictVO getByType(String typeCode) {
        Long userId = userIdProvider.getCurrentUserId();
        EffectiveDataScope scope = dataScopeManager.getEffectiveDataScope(userId);
        // 数据权限为“全部”或者尚未配置时可以复用全局缓存，否则按部门动态查询
        boolean useGlobalCache = scope == null || scope.isAll();
        if (useGlobalCache) {
            try {
                String cached = cache.getCachedDict(typeCode);
                if (cached != null) {
                    return om.readValue(cached, DictVO.class);
                }
            } catch (Exception ignore) {}
        }

        SysDictType t = typeMapper.selectOne(Wrappers.<SysDictType>lambdaQuery()
                .eq(SysDictType::getCode, typeCode));
        if (t == null || t.getStatus() == null || t.getStatus() != 1) {
            cache.evictDict(typeCode);
            return null;
        }

        LambdaQueryWrapper<SysDictItem> wrapper = Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getTypeCode, typeCode)
                .eq(SysDictItem::getStatus, 1);
        // 根据数据范围拼接部门过滤条件
        applyDeptScope(wrapper, scope, userId);
        List<SysDictItem> items = itemMapper.selectList(wrapper);

        DictVO vo = buildDictVO(t, items);

        if (useGlobalCache) {
            cacheDictSafely(typeCode, vo);
        }
        return vo;
    }

    @Override
    public void evictType(String typeCode) {
        cache.evictDict(typeCode);
    }

    @Override
    public void syncAllDictCache() {
        List<SysDictType> types = typeMapper.selectList(Wrappers.<SysDictType>lambdaQuery());
        if (types == null || types.isEmpty()) {
            return;
        }
        List<SysDictItem> allItems = itemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getStatus, 1));
        Map<String, List<SysDictItem>> grouped = allItems == null
                ? Collections.emptyMap()
                : allItems.stream().collect(Collectors.groupingBy(SysDictItem::getTypeCode));
        for (SysDictType type : types) {
            if (type == null || !StringUtils.hasText(type.getCode())) {
                continue;
            }
            List<SysDictItem> list = grouped.getOrDefault(type.getCode(), Collections.emptyList());
            refreshCache(type, list);
        }
    }

    @Override
    public void syncTypeCache(String typeCode) {
        if (!StringUtils.hasText(typeCode)) {
            return;
        }
        SysDictType type = typeMapper.selectOne(Wrappers.<SysDictType>lambdaQuery()
                .eq(SysDictType::getCode, typeCode));
        if (type == null) {
            cache.evictDict(typeCode);
            return;
        }
        List<SysDictItem> items = itemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                .eq(SysDictItem::getTypeCode, type.getCode())
                .eq(SysDictItem::getStatus, 1));
        refreshCache(type, items);
    }

    @Override
    public Page<SysDictItem> pageItems(DictItemPageQuery q, long pageNo, long pageSize) {
        Page<SysDictItem> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<SysDictItem> wrapper = Wrappers.lambdaQuery();
        if (q != null) {
            if (StringUtils.hasText(q.getTypeCode())) {
                wrapper.eq(SysDictItem::getTypeCode, q.getTypeCode().trim());
            }
            if (StringUtils.hasText(q.getLabel())) {
                wrapper.like(SysDictItem::getLabel, q.getLabel().trim());
            }
        }
        Long userId = userIdProvider.getCurrentUserId();
        EffectiveDataScope scope = dataScopeManager.getEffectiveDataScope(userId);
        // 列表分页同样需要受到数据范围控制
        applyDeptScope(wrapper, scope, userId);
        wrapper.orderByDesc(SysDictItem::getStatus)
                .orderByAsc(SysDictItem::getSort)
                .orderByAsc(SysDictItem::getId);
        Page<SysDictItem> result = itemMapper.selectPage(page, wrapper);
        List<SysDictItem> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            Map<Long, SysDept> deptMap = loadDeptMap(records);
            for (SysDictItem record : records) {
                record.setDept(buildDeptBrief(record.getDeptId(), deptMap));
            }
        }
        return result;
    }

    @Override
    public List<SysDictType> listTypes(DictTypePageQuery q) {
        LambdaQueryWrapper<SysDictType> wrapper = Wrappers.lambdaQuery();
        if (q != null) {
            String keyword = StringUtils.hasText(q.getKeyword()) ? q.getKeyword().trim() : null;
            if (StringUtils.hasText(keyword)) {
                wrapper.and(w -> w.like(SysDictType::getCode, keyword)
                        .or()
                        .like(SysDictType::getName, keyword));
            }
            if (q.getStatus() != null) {
                wrapper.eq(SysDictType::getStatus, q.getStatus());
            }
            if (q.getStartTime() != null) {
                wrapper.ge(SysDictType::getCreatedAt, q.getStartTime());
            }
            if (q.getEndTime() != null) {
                wrapper.le(SysDictType::getCreatedAt, q.getEndTime());
            }
        }
        wrapper.orderByAsc(SysDictType::getId);
        return typeMapper.selectList(wrapper);
    }

    private void refreshCache(SysDictType type, List<SysDictItem> items) {
        if (type == null || !StringUtils.hasText(type.getCode())) {
            return;
        }
        if (type.getStatus() == null || type.getStatus() != 1) {
            cache.evictDict(type.getCode());
            return;
        }
        DictVO vo = buildDictVO(type, items);
        cacheDictSafely(type.getCode(), vo);
    }

    private void cacheDictSafely(String typeCode, DictVO vo) {
        if (!StringUtils.hasText(typeCode)) {
            return;
        }
        if (vo == null) {
            cache.evictDict(typeCode);
            return;
        }
        try {
            cache.cacheDict(typeCode, om.writeValueAsString(vo));
        } catch (Exception ex) {
            log.warn("同步字典缓存失败 typeCode={} err={}", typeCode, ex.getMessage());
        }
    }

    private DictVO buildDictVO(SysDictType type, List<SysDictItem> items) {
        if (type == null || type.getStatus() == null || type.getStatus() != 1 || !StringUtils.hasText(type.getCode())) {
            return null;
        }
        List<SysDictItem> sorted = filterAndSort(items);
        Map<Long, SysDept> deptMap = loadDeptMap(sorted);

        DictVO vo = new DictVO();
        vo.setType(type.getCode());
        List<DictVO.Item> list = sorted.stream().map(i -> {
            DictVO.Item it = new DictVO.Item();
            it.setLabel(i.getLabel());
            it.setValue(i.getValue());
            it.setSort(i.getSort());
            it.setExt(i.getExt());
            it.setDept(buildDeptBrief(i.getDeptId(), deptMap));
            return it;
        }).toList();
        vo.setItems(list);
        return vo;
    }

    private List<SysDictItem> filterAndSort(List<SysDictItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        Comparator<SysDictItem> comparator = Comparator
                .comparing(SysDictItem::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SysDictItem::getId, Comparator.nullsLast(Long::compareTo));
        return items.stream()
                .filter(Objects::nonNull)
                .filter(i -> i.getStatus() != null && i.getStatus() == 1)
                .sorted(comparator)
                .toList();
    }

    /**
     * 根据当前用户的数据权限拼接部门过滤条件。
     * 允许查询公共字典项（dept_id 为空）以及授权部门下的数据。
     */
    private void applyDeptScope(LambdaQueryWrapper<SysDictItem> wrapper, EffectiveDataScope scope, Long userId) {
        if (wrapper == null || scope == null || scope.isAll()) {
            return;
        }
        Set<Long> deptIds = new LinkedHashSet<>(scope.getDeptIds());
        if (scope.isSelf()) {
            Long deptId = resolveDeptId(userId);
            if (deptId != null) {
                deptIds.add(deptId);
            }
        }
        if (deptIds.isEmpty()) {
            wrapper.and(w -> w.isNull(SysDictItem::getDeptId));
            return;
        }
        wrapper.and(w -> w.isNull(SysDictItem::getDeptId)
                .or()
                .in(SysDictItem::getDeptId, deptIds));
    }

    private Map<Long, SysDept> loadDeptMap(List<SysDictItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> deptIds = items.stream()
                .map(SysDictItem::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysDept> depts = deptMapper.selectBatchIds(new ArrayList<>(deptIds));
        if (depts == null || depts.isEmpty()) {
            return Collections.emptyMap();
        }
        return depts.stream().collect(Collectors.toMap(SysDept::getId, dept -> dept, (a, b) -> a));
    }

    private DeptBriefVO buildDeptBrief(Long deptId, Map<Long, SysDept> deptMap) {
        if (deptId == null || deptMap.isEmpty()) {
            return null;
        }
        SysDept dept = deptMap.get(deptId);
        if (dept == null) {
            return null;
        }
        DeptBriefVO brief = new DeptBriefVO();
        brief.setId(dept.getId());
        brief.setName(dept.getName());
        return brief;
    }

    /**
     * 查询用户所属部门，失败时返回 null 并记录日志，避免影响主流程。
     */
    private Long resolveDeptId(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            SysUser user = userMapper.selectById(userId);
            return user == null ? null : user.getDeptId();
        } catch (Exception ex) {
            log.warn("查询用户部门失败 userId={} err={}", userId, ex.getMessage());
            return null;
        }
    }
}
