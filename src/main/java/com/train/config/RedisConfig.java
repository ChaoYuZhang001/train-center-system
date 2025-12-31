package com.train.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RedisConfig {

    /**
     * 自定义RedisTemplate，配置JSON序列化器
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 设置Redis连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 1. 构建Jackson2JsonRedisSerializer序列化器（序列化Value）
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        // 配置ObjectMapper，让Jackson能访问类的所有字段（包括private）
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 支持序列化对象的类型信息（反序列化时能准确还原对象类型）
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        // 2. 构建StringRedisSerializer序列化器（序列化Key）
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // 3. 配置RedisTemplate的序列化规则
        redisTemplate.setKeySerializer(stringRedisSerializer); // Key使用String序列化
        redisTemplate.setHashKeySerializer(stringRedisSerializer); // Hash的Key使用String序列化
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer); // Value使用JSON序列化
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer); // Hash的Value使用JSON序列化

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}