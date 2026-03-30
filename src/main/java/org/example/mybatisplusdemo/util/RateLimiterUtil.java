package org.example.mybatisplusdemo.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RateLimiterUtil {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 限流检查
     * @param key 限流的key（比如 IP 或 用户名）
     * @param limit 限制次数
     * @param window 时间窗口（秒）
     * @return true=超过限制，false=未超过
     */
    public boolean isRateLimited(String key, int limit, int window) {
        String countKey = "rate:limit:" + key;

        // 获取当前计数
        String countStr = redisTemplate.opsForValue().get(countKey);
        long count = countStr == null ? 0 : Long.parseLong(countStr);

        if (count >= limit) {
            return true; // 超过限制
        }

        // 增加计数
        redisTemplate.opsForValue().increment(countKey);

        // 如果是第一次，设置过期时间
        if (count == 0) {
            redisTemplate.expire(countKey, window, TimeUnit.SECONDS);
        }

        return false;
    }

    /**
     * 获取剩余次数
     */
    public long getRemaining(String key, int limit) {
        String countStr = redisTemplate.opsForValue().get("rate:limit:" + key);
        long count = countStr == null ? 0 : Long.parseLong(countStr);
        return Math.max(0, limit - count);
    }

    /**
     * 重置限流（登录成功后清除失败记录）
     */
    public void reset(String key) {
        redisTemplate.delete("rate:limit:" + key);
    }
}