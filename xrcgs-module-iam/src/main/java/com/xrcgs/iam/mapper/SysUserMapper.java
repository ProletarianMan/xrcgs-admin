package com.xrcgs.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xrcgs.iam.entity.SysUser;
import com.xrcgs.iam.model.query.UserPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    Page<SysUser> selectPage(Page<SysUser> page, @Param("q") UserPageQuery query);
}
