package com.xrcgs.syslog.service;

import com.xrcgs.syslog.entity.SysOpLog;

public interface SysOpLogService {
    void saveSafe(SysOpLog log);
}
