package com.xrcgs.infrastructure.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * 配置MybatisPlus
 */
@Configuration
@MapperScan(
        basePackages = {
                "com.xrcgs.iam.mapper",   // 你原有 IAM 模块的 mapper
                "com.xrcgs.auth.user"     // 新增 auth 模块里的 SysUserMapper 包
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        var i = new MybatisPlusInterceptor();
        i.addInnerInterceptor(new PaginationInnerInterceptor());
        return i;
    }

}
