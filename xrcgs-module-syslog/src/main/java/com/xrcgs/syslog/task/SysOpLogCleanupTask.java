package com.xrcgs.syslog.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xrcgs.syslog.config.OpLogCleanupProperties;
import com.xrcgs.syslog.entity.SysOpLog;
import com.xrcgs.syslog.mapper.SysOpLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * sys_op_log 定时清理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysOpLogCleanupTask {

    private final SysOpLogMapper mapper;
    private final OpLogCleanupProperties props;

    /** 根据配置的 CRON 触发 */
    @Scheduled(cron = "#{@opLogCleanupProperties.cron}")
    public void clean() {
        if (!props.isEnabled()) {
            return;
        }

        int days = Math.max(props.getRetentionDays(), 1);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);

        try {
            int totalDeleted = 0;

            if (props.getBatchSize() > 0) {
                // 分批删除，避免长事务/大锁
                int batch;
                do {
                    batch = mapper.delete(new LambdaQueryWrapper<SysOpLog>()
                            .lt(SysOpLog::getCreatedAt, cutoff)
                            .last("LIMIT " + props.getBatchSize()));
                    totalDeleted += batch;
                } while (batch >= props.getBatchSize());
            } else {
                // 一次性删除（量小/有索引时可用）
                totalDeleted = mapper.delete(new LambdaQueryWrapper<SysOpLog>()
                        .lt(SysOpLog::getCreatedAt, cutoff));
            }

            log.info("sys_op_log 操作日志表完成: 保留天数={}, 截止={}, 删除行数={}",
                    days, cutoff, totalDeleted);
        } catch (Exception e) {
            log.error("sys_op_log 清理失败: {}", e.getMessage(), e);
        }
    }
}
