package com.xrcgs.infrastructure.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * 主库数据源配置：按照 MyBatis-Plus 官方多数据源实践，
 * 手动声明主数据源的连接池、SqlSessionFactory 与 SqlSessionTemplate。
 */
@Configuration
public class PrimaryDataSourceConfig {

    private final ObjectProvider<MybatisPlusInterceptor> interceptorProvider;
    private final MybatisPlusProperties mybatisPlusProperties;

    public PrimaryDataSourceConfig(ObjectProvider<MybatisPlusInterceptor> interceptorProvider,
                                   MybatisPlusProperties mybatisPlusProperties) {
        this.interceptorProvider = interceptorProvider;
        this.mybatisPlusProperties = mybatisPlusProperties;
    }

    @Bean(name = "primaryDataSourceProperties")
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "dataSource")
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource primaryDataSource(@Qualifier("primaryDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "sqlSessionFactory")
    @Primary
    public SqlSessionFactory primarySqlSessionFactory(@Qualifier("dataSource") DataSource dataSource) throws Exception {
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        var interceptor = interceptorProvider.getIfAvailable();
        if (interceptor != null) {
            factory.setPlugins(interceptor);
        }

        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);

        var coreConfiguration = mybatisPlusProperties.getConfiguration();
        if (coreConfiguration != null) {
            coreConfiguration.applyTo(configuration);
        }
        factory.setConfiguration(configuration);

        if (mybatisPlusProperties.getGlobalConfig() != null) {
            factory.setGlobalConfig(mybatisPlusProperties.getGlobalConfig());
        }
        if (StringUtils.hasText(mybatisPlusProperties.getTypeAliasesPackage())) {
            factory.setTypeAliasesPackage(mybatisPlusProperties.getTypeAliasesPackage());
        }
        if (StringUtils.hasText(mybatisPlusProperties.getTypeHandlersPackage())) {
            factory.setTypeHandlersPackage(mybatisPlusProperties.getTypeHandlersPackage());
        }
        if (StringUtils.hasText(mybatisPlusProperties.getTypeEnumsPackage())) {
            factory.setTypeEnumsPackage(mybatisPlusProperties.getTypeEnumsPackage());
        }
        if (!ObjectUtils.isEmpty(mybatisPlusProperties.getMapperLocations())) {
            factory.setMapperLocations(mybatisPlusProperties.resolveMapperLocations());
        }
        if (!ObjectUtils.isEmpty(mybatisPlusProperties.getConfigurationProperties())) {
            factory.setConfigurationProperties(mybatisPlusProperties.getConfigurationProperties());
        }

        return factory.getObject();
    }

    @Bean(name = "sqlSessionTemplate")
    @Primary
    public SqlSessionTemplate primarySqlSessionTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "transactionManager")
    @Primary
    public DataSourceTransactionManager primaryTransactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
