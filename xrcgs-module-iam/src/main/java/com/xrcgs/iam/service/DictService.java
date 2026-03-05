package com.xrcgs.iam.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xrcgs.iam.entity.SysDictItem;
import com.xrcgs.iam.entity.SysDictType;
import com.xrcgs.iam.model.query.DictItemPageQuery;
import com.xrcgs.iam.model.query.DictTypePageQuery;
import com.xrcgs.iam.model.vo.DictVO;

import java.util.List;
import java.util.Map;

public interface DictService {
    Long createType(SysDictType type);
    void updateType(SysDictType type);
    void removeType(Long id);

    Long createItem(SysDictItem item);
    void updateItem(SysDictItem item);
    void removeItem(Long id);

    DictVO getByType(String typeCode);
    DictVO getByType(String typeCode, Long filterDeptId);
    Map<String, DictVO> getByTypes(List<String> typeCodes, Long filterDeptId);
    void evictType(String typeCode);

    void syncAllDictCache();

    void syncTypeCache(String typeCode);

    Page<SysDictItem> pageItems(DictItemPageQuery q, long pageNo, long pageSize);

    List<SysDictType> listTypes(DictTypePageQuery q);
}
