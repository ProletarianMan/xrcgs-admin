package com.xrcgs.syslog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xrcgs.syslog.entity.SysOpLog;
import com.xrcgs.syslog.mapper.SysOpLogMapper;
import com.xrcgs.syslog.model.query.SysOpLogPageQuery;
import com.xrcgs.syslog.service.SysOpLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

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

    @Override
    @Transactional(readOnly = true)
    public Page<SysOpLog> page(SysOpLogPageQuery query, long pageNo, long pageSize) {
        SysOpLogPageQuery actualQuery = query == null ? new SysOpLogPageQuery() : query;
        LambdaQueryWrapper<SysOpLog> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(actualQuery.getTitle())) {
            wrapper.like(SysOpLog::getTitle, actualQuery.getTitle());
        }

        LocalDateTime start = actualQuery.getStartTime();
        LocalDateTime end = actualQuery.getEndTime();
        if (start != null && end != null && end.isBefore(start)) {
            LocalDateTime tmp = start;
            start = end;
            end = tmp;
        }
        if (start != null) {
            wrapper.ge(SysOpLog::getCreatedAt, start);
        }
        if (end != null) {
            wrapper.le(SysOpLog::getCreatedAt, end);
        }

        wrapper.orderByDesc(SysOpLog::getCreatedAt);
        Page<SysOpLog> page = Page.of(pageNo, pageSize);
        return mapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public SysOpLog get(Long id) {
        return mapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }
        return mapper.deleteBatchIds(ids) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int clearAll() {
        return mapper.delete(Wrappers.lambdaQuery());
    }
}
