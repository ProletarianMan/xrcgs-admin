package com.xrcgs.iam.service;

import com.xrcgs.iam.model.dto.PermissionUpsertDTO;
import com.xrcgs.iam.model.vo.PermissionVO;

import java.util.List;

public interface PermissionService {
    /**
     * 按名称查询独立权限
     */
    List<PermissionVO> list(String name);

    Long create(PermissionUpsertDTO dto);

    void update(Long id, PermissionUpsertDTO dto);

    void remove(List<Long> ids);
}
