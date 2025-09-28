package com.xrcgs.iam.service;

import com.xrcgs.iam.model.vo.PermissionVO;

import java.util.List;

public interface PermissionService {
    /**
     * 查询所有独立权限
     */
    List<PermissionVO> listAll();
}
