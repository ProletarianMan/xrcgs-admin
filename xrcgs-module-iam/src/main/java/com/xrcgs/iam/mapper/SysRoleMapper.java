package com.xrcgs.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xrcgs.iam.entity.SysRole;
import com.xrcgs.iam.model.query.RolePageQuery;
import org.apache.ibatis.annotations.Param;

public interface SysRoleMapper extends BaseMapper<SysRole> {
    Page<SysRole> selectPage(Page<SysRole> page, @Param("q") RolePageQuery q);
}
