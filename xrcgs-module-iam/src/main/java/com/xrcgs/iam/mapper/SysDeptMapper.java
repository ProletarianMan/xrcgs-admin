package com.xrcgs.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xrcgs.iam.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    List<SysDept> selectChildren(@Param("parentId") Long parentId);

    List<SysDept> selectByPathPrefix(@Param("pathPrefix") String pathPrefix);

    List<Long> selectIdsByPathPrefix(@Param("pathPrefix") String pathPrefix);

    int updatePathPrefix(@Param("oldPath") String oldPath, @Param("newPath") String newPath);

    Long countChildren(@Param("parentId") Long parentId);
}
