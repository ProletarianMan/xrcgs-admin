package com.xrcgs.infrastructure.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * 当默认的 SqlSessionFactory 未由 MyBatis-Plus 自动配置提供时，
 * 为主业务数据源补上一份兜底配置，避免 Mapper 装配失败。
 */
@Configuration
@EnableConfigurationProperties(MybatisPlusProperties.class)
@ConditionalOnMissingBean(name = "sqlSessionFactory")
public class PrimaryMybatisFallbackConfig {

    private final DataSource dataSource;
    private final ObjectProvider<MybatisPlusInterceptor> interceptorProvider;
    private final ObjectProvider<MybatisPlusProperties> propertiesProvider;
    private final ObjectProvider<MetaObjectHandler> metaObjectHandlerProvider;

    public PrimaryMybatisFallbackConfig(@Qualifier("dataSource") DataSource dataSource,
                                        ObjectProvider<MybatisPlusInterceptor> interceptorProvider,
                                        ObjectProvider<MybatisPlusProperties> propertiesProvider,
                                        ObjectProvider<MetaObjectHandler> metaObjectHandlerProvider) {
        this.dataSource = dataSource;
        this.interceptorProvider = interceptorProvider;
        this.propertiesProvider = propertiesProvider;
        this.metaObjectHandlerProvider = metaObjectHandlerProvider;
    }

    @Bean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        var interceptor = interceptorProvider.getIfAvailable();
        if (interceptor != null) {
            factory.setPlugins(interceptor);
        }

        MybatisPlusProperties properties = propertiesProvider.getIfAvailable(MybatisPlusProperties::new);

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);

        var coreConfiguration = properties.getConfiguration();
        if (coreConfiguration != null) {
            coreConfiguration.applyTo(configuration);
        }
        factory.setConfiguration(configuration);

        var globalConfig = resolveGlobalConfig(properties);
        if (globalConfig != null) {
            factory.setGlobalConfig(globalConfig);
        }
        if (StringUtils.hasText(properties.getTypeAliasesPackage())) {
            factory.setTypeAliasesPackage(properties.getTypeAliasesPackage());
        }
        if (StringUtils.hasText(properties.getTypeHandlersPackage())) {
            factory.setTypeHandlersPackage(properties.getTypeHandlersPackage());
        }
        if (StringUtils.hasText(properties.getTypeEnumsPackage())) {
            factory.setTypeEnumsPackage(properties.getTypeEnumsPackage());
        }
        if (!ObjectUtils.isEmpty(properties.getMapperLocations())) {
            factory.setMapperLocations(properties.resolveMapperLocations());
        }
        if (!ObjectUtils.isEmpty(properties.getConfigurationProperties())) {
            factory.setConfigurationProperties(properties.getConfigurationProperties());
        }

        return factory.getObject();
    }

    @Bean(name = "sqlSessionTemplate")
    @ConditionalOnMissingBean
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    private GlobalConfig resolveGlobalConfig(MybatisPlusProperties properties) {
        GlobalConfig config = properties.getGlobalConfig();
        if (config == null) {
            config = new GlobalConfig();
        }
        if (config.getMetaObjectHandler() == null) {
            MetaObjectHandler handler = metaObjectHandlerProvider.getIfAvailable();
            if (handler != null) {
                config.setMetaObjectHandler(handler);
            }
        }
        return config;
    }
}
