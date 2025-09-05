package com.xrcgs.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xrcgs.iam.entity.SysDictItem;
import com.xrcgs.iam.model.query.DictItemPageQuery;
import org.apache.ibatis.annotations.Param;

public interface SysDictItemMapper extends BaseMapper<SysDictItem> {

    /**
     * 只查 del_flag=0 的有效记录；
     * 优先返回启用项（status=1），再按 sort、id 排序；
     * typeCode 精确，label 模糊。
     */
    Page<SysDictItem> selectPageByQuery(Page<SysDictItem> page,
                                        @Param("q") DictItemPageQuery q);

}
