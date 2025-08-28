package com.xrcgs.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.xrcgs")
@ConfigurationPropertiesScan(basePackages = "com.xrcgs") // 关键：让 @ConfigurationProperties 生效
public class XrcgsAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(XrcgsAdminApplication.class, args);
    }
}
