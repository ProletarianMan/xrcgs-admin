package com.xrcgs.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

@SpringBootApplication(scanBasePackages = "com.xrcgs")
@ConfigurationPropertiesScan(basePackages = "com.xrcgs") // 关键：让 @ConfigurationProperties 生效
@MapperScan(
        basePackages = {
                "com.xrcgs.iam.mapper",
                "com.xrcgs.syslog.mapper",
                "com.xrcgs.file.mapper",
                "com.xrcgs.auth.user"
        },
        sqlSessionFactoryRef = "sqlSessionFactory",
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
public class XrcgsAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(XrcgsAdminApplication.class, args);
    }
}
