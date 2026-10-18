package com.ruoyi.framework.oauth;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

interface QuantaOAuthRateLimiter
{
    boolean allow(String action, String ip);

    final class Memory implements QuantaOAuthRateLimiter
    {
        private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();

        @Override
        public boolean allow(String action, String ip)
        {
            return counts.computeIfAbsent(action + ":" + ip, key -> new AtomicInteger()).incrementAndGet() <= 60;
        }
    }
}

@Component
class RedisQuantaOAuthRateLimiter implements QuantaOAuthRateLimiter
{
    private final StringRedisTemplate redis;

    RedisQuantaOAuthRateLimiter(StringRedisTemplate redis)
    {
        this.redis = redis;
    }

    @Override
    public boolean allow(String action, String ip)
    {
        String key = "oauth:rl:" + action + ":" + (ip == null ? "unknown" : ip);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L)
        {
            redis.expire(key, Duration.ofMinutes(1));
        }
        return count != null && count <= 60L;
    }
}
