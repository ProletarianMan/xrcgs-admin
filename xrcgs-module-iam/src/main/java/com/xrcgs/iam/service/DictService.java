package com.xrcgs.iam.service;

import com.xrcgs.iam.entity.SysDictItem;
import com.xrcgs.iam.entity.SysDictType;
import com.xrcgs.iam.model.vo.DictVO;

import java.util.List;

public interface DictService {
    Long createType(SysDictType type);
    void updateType(SysDictType type);
    void removeType(Long id);

    Long createItem(SysDictItem item);
    void updateItem(SysDictItem item);
    void removeItem(Long id);

    DictVO getByType(String typeCode); // 带缓存
    void evictType(String typeCode); // 逐出类型
}
