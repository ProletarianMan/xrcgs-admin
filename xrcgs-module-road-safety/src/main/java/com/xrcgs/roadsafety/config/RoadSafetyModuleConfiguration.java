package com.xrcgs.roadsafety.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * 基础配置入口，提供路产安全模块的组件扫描能力。
 */
@Configuration
@ComponentScan(basePackages = "com.xrcgs.roadsafety")
@MapperScan(
        basePackages = "com.xrcgs.roadsafety.**.mapper",
        sqlSessionTemplateRef = "roadSafetySqlSessionTemplate",
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
public class RoadSafetyModuleConfiguration {
}
