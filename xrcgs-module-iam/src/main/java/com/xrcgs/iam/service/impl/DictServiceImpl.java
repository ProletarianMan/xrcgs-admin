package com.xrcgs.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xrcgs.iam.entity.SysDictItem;
import com.xrcgs.iam.entity.SysDictType;
import com.xrcgs.iam.mapper.SysDictItemMapper;
import com.xrcgs.iam.mapper.SysDictTypeMapper;
import com.xrcgs.iam.model.query.DictItemPageQuery;
import com.xrcgs.iam.model.query.DictTypePageQuery;
import com.xrcgs.iam.model.vo.DictVO;
import com.xrcgs.iam.service.DictService;
import com.xrcgs.common.cache.AuthCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    private final SysDictTypeMapper typeMapper;
    private final SysDictItemMapper itemMapper;
    private final AuthCacheService cache;
    private final ObjectMapper om = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createType(SysDictType type) {
        Long c = typeMapper.selectCount(Wrappers.<SysDictType>lambdaQuery().eq(SysDictType::getCode, type.getCode()));
        if (c != null && c > 0) throw new IllegalArgumentException("字典类型已存在: " + type.getCode());
        if (type.getStatus() == null) type.setStatus(1);
        typeMapper.insert(type);
        return type.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateType(SysDictType type) {
        typeMapper.updateById(type);
        if (type.getCode() != null) cache.evictDict(type.getCode());
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
        itemMapper.insert(item);
        cache.evictDict(item.getTypeCode());
        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(SysDictItem item) {
        itemMapper.updateById(item);
        cache.evictDict(item.getTypeCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long id) {
        SysDictItem item = itemMapper.selectById(id);
        if (item != null) {
            itemMapper.deleteById(id);
            cache.evictDict(item.getTypeCode());
        }
    }

    @Override
    public DictVO getByType(String typeCode) {
        try {
            String cached = cache.getCachedDict(typeCode);
            if (cached != null) {
                return om.readValue(cached, DictVO.class);
            }
        } catch (Exception ignore) {}

        SysDictType t = typeMapper.selectOne(Wrappers.<SysDictType>lambdaQuery()
                .eq(SysDictType::getCode, typeCode));
        if (t == null || t.getStatus() != 1) return null;

        List<SysDictItem> items = itemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                        .eq(SysDictItem::getTypeCode, typeCode)
                        .eq(SysDictItem::getStatus, 1))
                .stream()
                .sorted(Comparator.comparing(SysDictItem::getSort).thenComparing(SysDictItem::getId))
                .toList();

        DictVO vo = new DictVO();
        vo.setType(typeCode);
        List<DictVO.Item> list = items.stream().map(i -> {
            DictVO.Item it = new DictVO.Item();
            it.setLabel(i.getLabel());
            it.setValue(i.getValue());
            it.setSort(i.getSort());
            it.setExt(i.getExt());
            return it;
        }).toList();
        vo.setItems(list);

        try {
            cache.cacheDict(typeCode, om.writeValueAsString(vo));
        } catch (Exception ignore) {}
        return vo;
    }

    @Override
    public void evictType(String typeCode) {
        cache.evictDict(typeCode);
    }

    @Override
    public Page<SysDictItem> pageItems(DictItemPageQuery q, long pageNo, long pageSize) {
        Page<SysDictItem> page = new Page<>(pageNo, pageSize);
        return itemMapper.selectPageByQuery(page, q);
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
                wrapper.ge(SysDictType::getCreateTime, q.getStartTime());
            }
            if (q.getEndTime() != null) {
                wrapper.le(SysDictType::getCreateTime, q.getEndTime());
            }
        }
        wrapper.orderByAsc(SysDictType::getId);
        return typeMapper.selectList(wrapper);
    }
}
