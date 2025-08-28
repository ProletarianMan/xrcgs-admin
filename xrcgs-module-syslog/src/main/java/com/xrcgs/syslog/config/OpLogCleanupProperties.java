package com.xrcgs.syslog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 操作日志清理配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "syslog.cleanup")
public class OpLogCleanupProperties {
    /** 是否启用清理任务 */
    private boolean enabled = true;
    /** 保留天数（超过的会被清理） */
    private int retentionDays = 90;
    /** CRON 表达式（默认每天 03:30 执行） */
    private String cron = "0 30 3 * * ?";
    /** 单次最大删除行数（避免长事务；<=0 表示不分批，直接删） */
    private int batchSize = 0;
}
