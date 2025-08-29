package com.xrcgs.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xrcgs.file.model.entity.SysFile;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface SysFileMapper extends BaseMapper<SysFile> {
    // 不写任何 @Select，自定义统计改到 Service 用 Wrapper 完成
}
