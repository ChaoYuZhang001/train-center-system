package com.train.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 存入Redis键值对（带过期时间）
     * @param key 键
     * @param value 值
     * @param timeout 过期时间
     * @param timeUnit 时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 判断Redis中是否存在该键
     * @param key 键
     * @return true=存在，false=不存在
     */
    public boolean hasKey(String key) {
        // 此处若RedisTemplate为静态注入，可直接调用；非静态可调整为实例方法
        return redisTemplate.hasKey(key);
    }

    /**
     * 获取Redis中的值
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }


    /**
     * 删除Redis中的键
     * @param key 键
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }
    /**
     * 批量删除Redis中的键
     * @param keys 键列表
     */
    public void batchDelete(List<String> keys) {
        redisTemplate.delete(keys);
    }

    /**
     * 如果key不存在，则设置key-value并设置过期时间
     */
    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
        return result != null && result;
    }

    /**
     * 删除键值对，仅当值等于预期值时
     */
    public boolean deleteIfEquals(String key, Object value) {
        Boolean result = (Boolean) redisTemplate.opsForValue().getAndDelete(key);
        return result != null && result.equals(value);
    }

    /**
     * 判断键值对是否存在
     */
    public boolean exists(String oldSessionKey) {
        return redisTemplate.hasKey(oldSessionKey);
    }

    public boolean expire(String key,  long timeout, final  TimeUnit seconds) {
        return redisTemplate.expire(key, timeout, seconds);
    }
}