package com.ruoyi.framework.oauth;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

interface QuantaOAuthCodeStore
{
    void save(String code, QuantaOAuthCode value);

    QuantaOAuthCode consume(String code);

    final class Memory implements QuantaOAuthCodeStore
    {
        private final Map<String, QuantaOAuthCode> codes = new ConcurrentHashMap<>();

        @Override
        public void save(String code, QuantaOAuthCode value)
        {
            codes.put(code, value);
        }

        @Override
        public QuantaOAuthCode consume(String code)
        {
            QuantaOAuthCode value = codes.remove(code);
            if (value == null || value.expiresAt <= System.currentTimeMillis())
            {
                return null;
            }
            return value;
        }

        int size()
        {
            return codes.size();
        }
    }
}

@Component
class RedisQuantaOAuthCodeStore implements QuantaOAuthCodeStore
{
    private static final String CONSUME = """
            local value = redis.call('GET', KEYS[1])
            if not value then return nil end
            redis.call('DEL', KEYS[1])
            return value
            """;

    private final StringRedisTemplate redis;

    RedisQuantaOAuthCodeStore(StringRedisTemplate redis)
    {
        this.redis = redis;
    }

    @Override
    public void save(String code, QuantaOAuthCode value)
    {
        String payload = String.join("\n", value.clientId, value.redirectUri, value.userId, value.nonce,
                value.codeChallenge, Long.toString(value.expiresAt));
        redis.opsForValue().set(key(code), payload, Duration.ofSeconds(60));
    }

    @Override
    public QuantaOAuthCode consume(String code)
    {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setResultType(String.class);
        script.setScriptText(CONSUME);
        String payload = redis.execute(script, List.of(key(code)));
        if (payload == null || payload.isBlank())
        {
            return null;
        }
        String[] parts = payload.split("\n", 6);
        if (parts.length != 6)
        {
            return null;
        }
        QuantaOAuthCode value = new QuantaOAuthCode();
        value.clientId = parts[0];
        value.redirectUri = parts[1];
        value.userId = parts[2];
        value.nonce = parts[3];
        value.codeChallenge = parts[4];
        value.expiresAt = Long.parseLong(parts[5]);
        if (value.expiresAt <= System.currentTimeMillis())
        {
            return null;
        }
        return value;
    }

    private static String key(String code)
    {
        return "oauth:code:" + code;
    }
}
