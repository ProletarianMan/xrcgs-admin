package com.xrcgs.roadsafety.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * 当未显式配置路产安全独立数据源时，让模块回退到主库的 SqlSessionTemplate。
 */
@Configuration
@ConditionalOnMissingBean(name = "roadSafetySqlSessionTemplate")
@MapperScan(
        basePackages = "com.xrcgs.roadsafety.**.mapper",
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
public class RoadSafetyMapperFallbackConfiguration {
}
