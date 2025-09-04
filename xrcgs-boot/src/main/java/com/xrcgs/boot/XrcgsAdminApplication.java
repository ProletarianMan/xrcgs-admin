package com.xrcgs.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@EnableMethodSecurity   // 关键：启用 @PreAuthorize 等方法安全拦截
@SpringBootApplication(scanBasePackages = "com.xrcgs")
@ConfigurationPropertiesScan(basePackages = "com.xrcgs") // 关键：让 @ConfigurationProperties 生效
@MapperScan(
        basePackages = {
                "com.xrcgs.*.mapper",   // 你原有 IAM 模块的 mapper
                "com.xrcgs.auth.user"     // 新增 auth 模块里的 SysUserMapper 包
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
public class XrcgsAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(XrcgsAdminApplication.class, args);
    }
}
