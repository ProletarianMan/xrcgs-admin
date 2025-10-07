package com.xrcgs.roadsafety.inspection.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 巡查日志相关配置项。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "road-safety")
public class InspectionLogProperties {

    /**
     * 巡查日志 Excel 文件在磁盘上的持久化目录（绝对路径或相对项目根路径）。
     */
    private String inspectionLog;
}
