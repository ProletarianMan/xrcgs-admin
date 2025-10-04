package com.xrcgs.syslog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xrcgs.syslog.entity.SysOpLog;
import com.xrcgs.syslog.model.query.SysOpLogPageQuery;

import java.util.List;

public interface SysOpLogService {

    void saveSafe(SysOpLog log);

    Page<SysOpLog> page(SysOpLogPageQuery query, long pageNo, long pageSize);

    SysOpLog get(Long id);

    boolean deleteByIds(List<Long> ids);

    int clearAll();
}
