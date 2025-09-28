package com.xrcgs.iam.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xrcgs.iam.model.dto.RoleGrantMenuDTO;
import com.xrcgs.iam.model.dto.RoleGrantPermDTO;
import com.xrcgs.iam.model.dto.RoleUpsertDTO;
import com.xrcgs.iam.model.query.RolePageQuery;
import com.xrcgs.iam.model.vo.RolePageVO;

import java.util.List;

public interface RoleService {
    Long upsert(RoleUpsertDTO dto);
    void remove(Long roleId);
    void grantMenus(RoleGrantMenuDTO dto);
    void grantPerms(RoleGrantPermDTO dto);
    Page<RolePageVO> page(RolePageQuery q, long pageNo, long pageSize);
    List<Long> listMenuIdsByRole(Long roleId);
    List<Long> listPermIdsByRole(Long roleId);
}
