package com.xrcgs.syslog.service.impl;

import com.xrcgs.syslog.entity.SysOpLog;
import com.xrcgs.syslog.mapper.SysOpLogMapper;
import com.xrcgs.syslog.service.SysOpLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysOpLogServiceImpl implements SysOpLogService {
    private final SysOpLogMapper mapper;

    @Override
    public void saveSafe(SysOpLog opLog) {
        try {
            mapper.insert(opLog);
        } catch (Exception e) {
            // 打印完整堆栈，而不是只打印 e.getMessage()（很多驱动会返回 null）
            log.warn("sys_op_log insert failed. payload(title={}, uri={}, user={})",
                    opLog.getTitle(), opLog.getUri(), opLog.getUsername(), e);
        }
    }
}
