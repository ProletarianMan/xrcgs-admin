package com.xrcgs.roadsafety.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 基础配置入口，提供路产安全模块的组件扫描能力。
 */
@Configuration
@ComponentScan(basePackages = "com.xrcgs.roadsafety")
public class RoadSafetyModuleConfiguration {
}
