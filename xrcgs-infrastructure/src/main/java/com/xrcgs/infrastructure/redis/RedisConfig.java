package com.xrcgs.infrastructure.redis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

/**
 * 统一 Redis 配置（序列化防乱码）
 * 简单 KV/Set/SortedSet → StringRedisTemplate（无编码问题，最轻量）。
 * 需要缓存对象/列表 → RedisTemplate<String,Object>（JSON 序列化）。
 */
@Configuration
public class RedisConfig {

    /** 统一的 ObjectMapper：支持 JSR-310 时间类型、保持字段可见性、禁用时间戳写法 */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // om.activateDefaultTyping(om.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        // 上行如有多态需求再开启；默认先关闭，保持安全
        return om;
    }

    /** key/val = String/String（最常用，天然避免中文乱码） */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    /** key=String, value=JSON（对象缓存时使用） */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory factory, ObjectMapper redisObjectMapper) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // key、hashKey 用字符串序列化（UTF-8）
        StringRedisSerializer keySer = new StringRedisSerializer();
        template.setKeySerializer(keySer);
        template.setHashKeySerializer(keySer);

        // value、hashValue 用 Jackson JSON 序列化
        Jackson2JsonRedisSerializer<Object> valSer =
                new Jackson2JsonRedisSerializer<>(redisObjectMapper, Object.class);

        template.setValueSerializer(valSer);
        template.setHashValueSerializer(valSer);

        template.afterPropertiesSet();
        return template;
    }
}
