package com.jzp.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.jzp.serializer.JodaDateTimeJsonDeserializer;
import com.jzp.serializer.JodaDateTimeJsonSerializer;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.stereotype.Component;

/**
 * FileName:    RedisConfiguration
 * Author:      jzp
 * Date:        2020/6/10 17:26
 * Description: 基于 Redis 的 Session 配置和基本 Redis 配置
 */
@Component
/* 设置 Session 的最大时间为 3600 秒 */
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600)
public class RedisConfiguration {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        /* 设置 Connection Factory */
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        /* 设置 Key 序列化为 */
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        /* 创建 SimpleModule 对象，并设置对 DateTime 做特殊的序列化处理 */
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(DateTime.class, new JodaDateTimeJsonSerializer());
        simpleModule.addDeserializer(DateTime.class, new JodaDateTimeJsonDeserializer());
        /* 创建 ObjectMapper 对象 */
        ObjectMapper objectMapper = new ObjectMapper();
        /* 给 ObjectMapper 开启默认类型 */
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        /* 给 ObjectMapper 注册 SimpleModule */
        objectMapper.registerModule(simpleModule);
        /* 创建 Jackson2JsonRedisSerializer 对象 */
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        /* 并绑定 ObjectMapper */
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        /* 设置 Value 序列化方式为 jackson2JsonRedisSerializer */
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        /* 返回 RedisTemplate 对象 */
        return redisTemplate;
    }
}
