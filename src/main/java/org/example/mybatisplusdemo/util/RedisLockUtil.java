package org.example.mybatisplusdemo.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisLockUtil {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 尝试获取分布式锁
     * @param lockKey 锁的key
     * @param requestId 请求标识（用于区分谁加的锁）
     * @param expireTime 过期时间（秒）
     * @return true=获取成功，false=获取失败
     */
    public boolean tryLock(String lockKey, String requestId, long expireTime) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, requestId, expireTime, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放分布式锁（只有自己的锁才能释放）
     * @param lockKey 锁的key
     * @param requestId 请求标识
     */
    public void unlock(String lockKey, String requestId) {
        String value = redisTemplate.opsForValue().get(lockKey);
        if (requestId.equals(value)) {
            redisTemplate.delete(lockKey);
        }
    }
}