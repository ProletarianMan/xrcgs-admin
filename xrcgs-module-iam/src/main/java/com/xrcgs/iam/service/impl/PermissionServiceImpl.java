package com.xrcgs.iam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xrcgs.iam.entity.SysPermission;
import com.xrcgs.iam.mapper.SysPermissionMapper;
import com.xrcgs.iam.model.vo.PermissionVO;
import com.xrcgs.iam.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final SysPermissionMapper permissionMapper;

    @Override
    public List<PermissionVO> listAll() {
        List<SysPermission> permissions = permissionMapper.selectList(
                Wrappers.<SysPermission>lambdaQuery().orderByAsc(SysPermission::getId)
        );
        return permissions.stream().map(permission -> {
            PermissionVO vo = new PermissionVO();
            vo.setId(permission.getId());
            vo.setCode(permission.getCode());
            vo.setName(permission.getName());
            return vo;
        }).collect(Collectors.toList());
    }
}
