package com.xrcgs.infrastructure.jod;

import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.nio.file.Files;
import java.nio.file.Path;


/**
 * office文档转PDF文档转换器
 */
@Configuration
public class JodConverterConfig {

    @Value("${libreoffice.home:}")
    private String officeHome;

    @Value("${convert.timeout-ms:120000}")
    private long taskTimeoutMs;

    @Value("${convert.pool-size:2}")
    private int poolSize;

    /** OfficeManager：注意不再调用 .install()，避免自动下载/更新 */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public OfficeManager officeManager() {
        LocalOfficeManager.Builder builder = LocalOfficeManager.builder()
                .taskExecutionTimeout(taskTimeoutMs)
                .portNumbers(20010)           // 单端口更稳定
                .maxTasksPerProcess(200)
                .processTimeout(180000L)
                .startFailFast(true);         // 启动失败尽快抛出

        if (officeHome != null && !officeHome.isBlank()) {
            // 校验：必须指向 LibreOffice 的“根目录”（包含 program 子目录），而不是 program 目录本身
            Path home = Path.of(officeHome);
            if (!Files.exists(home)) {
                throw new IllegalStateException("libreoffice.home 不存在: " + home);
            }
            builder.officeHome(officeHome);
        }
        return builder.build();
    }

    /** 使用 LocalConverter.builder() 创建 DocumentConverter */
    @Bean
    public DocumentConverter documentConverter(OfficeManager officeManager) {
        return LocalConverter.builder()
                .officeManager(officeManager)
                .build();
    }

    @Bean("convertExecutor")
    @Primary
    public TaskExecutor convertExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(poolSize);
        ex.setMaxPoolSize(poolSize);
        ex.setQueueCapacity(2000);
        ex.setThreadNamePrefix("doc-convert-");
        ex.initialize();
        return ex;
    }
}
