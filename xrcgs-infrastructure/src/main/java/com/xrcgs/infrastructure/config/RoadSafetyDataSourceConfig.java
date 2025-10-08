package com.xrcgs.infrastructure.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.MybatisConfiguration;

/**
 * 数据源配置：为路产安全模块提供独立的数据源、会话工厂与事务管理。
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.datasource.road-safety", name = "url")
public class RoadSafetyDataSourceConfig {

    private final MybatisPlusInterceptor mybatisPlusInterceptor;
    private final MybatisPlusProperties mybatisPlusProperties;
    private final MetaObjectHandler metaObjectHandler;

    public RoadSafetyDataSourceConfig(MybatisPlusInterceptor mybatisPlusInterceptor,
                                      MybatisPlusProperties mybatisPlusProperties,
                                      MetaObjectHandler metaObjectHandler) {
        this.mybatisPlusInterceptor = mybatisPlusInterceptor;
        this.mybatisPlusProperties = mybatisPlusProperties;
        this.metaObjectHandler = metaObjectHandler;
    }

    @Bean
    @ConfigurationProperties("spring.datasource.road-safety")
    public DataSourceProperties roadSafetyDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "roadSafetyDataSource")
    @ConfigurationProperties("spring.datasource.road-safety.hikari")
    public DataSource roadSafetyDataSource(@Qualifier("roadSafetyDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "roadSafetySqlSessionFactory")
    public SqlSessionFactory roadSafetySqlSessionFactory(@Qualifier("roadSafetyDataSource") DataSource dataSource) throws Exception {
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPlugins(mybatisPlusInterceptor);

        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(configuration);

        var globalConfig = resolveGlobalConfig();
        if (globalConfig != null) {
            factory.setGlobalConfig(globalConfig);
        }

        if (StringUtils.hasText(mybatisPlusProperties.getTypeAliasesPackage())) {
            factory.setTypeAliasesPackage(mybatisPlusProperties.getTypeAliasesPackage());
        }
        if (StringUtils.hasText(mybatisPlusProperties.getTypeHandlersPackage())) {
            factory.setTypeHandlersPackage(mybatisPlusProperties.getTypeHandlersPackage());
        }

        var mapperLocations = resolveMapperLocations("classpath*:mapper/roadsafety/**/*.xml");
        if (mapperLocations.length > 0) {
            factory.setMapperLocations(mapperLocations);
        }

        return factory.getObject();
    }

    @Bean(name = "roadSafetySqlSessionTemplate")
    public SqlSessionTemplate roadSafetySqlSessionTemplate(@Qualifier("roadSafetySqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "roadSafetyTransactionManager")
    public DataSourceTransactionManager roadSafetyTransactionManager(@Qualifier("roadSafetyDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    private Resource[] resolveMapperLocations(String... mapperLocations) throws IOException {
        List<Resource> resources = new ArrayList<>();
        var resolver = new PathMatchingResourcePatternResolver();
        for (String mapperLocation : mapperLocations) {
            if (!StringUtils.hasText(mapperLocation)) {
                continue;
            }
            resources.addAll(List.of(resolver.getResources(mapperLocation)));
        }
        return resources.toArray(new Resource[0]);
    }

    private GlobalConfig resolveGlobalConfig() {
        GlobalConfig config = mybatisPlusProperties.getGlobalConfig();
        if (config == null) {
            config = new GlobalConfig();
        }
        if (config.getMetaObjectHandler() == null) {
            config.setMetaObjectHandler(metaObjectHandler);
        }
        return config;
    }
}
