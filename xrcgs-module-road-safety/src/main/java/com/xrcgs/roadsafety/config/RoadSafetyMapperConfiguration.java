package com.xrcgs.roadsafety.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * 在独立路产安全数据源启用时，将 Mapper 绑定到专属 SqlSessionTemplate。
 */
@Configuration
@ConditionalOnBean(name = "roadSafetySqlSessionTemplate")
@MapperScan(
        basePackages = "com.xrcgs.roadsafety.**.mapper",
        sqlSessionTemplateRef = "roadSafetySqlSessionTemplate",
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
public class RoadSafetyMapperConfiguration {
}
