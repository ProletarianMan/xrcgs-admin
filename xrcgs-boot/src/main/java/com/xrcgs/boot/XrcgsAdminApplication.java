package com.xrcgs.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.xrcgs")
public class XrcgsAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(XrcgsAdminApplication.class, args);
    }
}
