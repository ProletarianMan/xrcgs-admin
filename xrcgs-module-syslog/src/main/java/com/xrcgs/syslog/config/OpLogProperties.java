package com.xrcgs.syslog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 日志AOP切面，属性配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "syslog")
public class OpLogProperties {
    /** 总开关，默认 true */
    private boolean enabled = true;
    /** 入参/出参最大字符数（截断），默认 2048 */
    private int maxBodyLength = 2048;
    /** 是否记录出参（可全局开关，细粒度以注解为准） */
    private boolean logResult = true;
}
