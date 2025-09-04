package com.xrcgs.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xrcgs.iam.entity.SysMenu;
import com.xrcgs.iam.model.query.MenuQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysMenuMapper extends BaseMapper<SysMenu> {
    List<SysMenu> selectListByQuery(@Param("q") MenuQuery q);
    List<SysMenu> selectByRoleId(@Param("roleId") Long roleId);
}
