package com.xrcgs.auth.jwt;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;


/**
 * jwt相关配置操作
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtAutoConfig {}
