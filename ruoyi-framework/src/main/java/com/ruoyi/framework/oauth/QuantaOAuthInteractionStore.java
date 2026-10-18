package com.ruoyi.framework.oauth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

interface QuantaOAuthInteractionStore
{
    void create(QuantaOAuthInteraction interaction);

    QuantaOAuthInteraction find(String id);

    String approve(String id, String userId, long nowMillis);

    String deny(String id, long nowMillis);

    String consume(String id, long nowMillis);

    final class Memory implements QuantaOAuthInteractionStore
    {
        private final Map<String, QuantaOAuthInteraction> items = new ConcurrentHashMap<>();

        @Override
        public synchronized void create(QuantaOAuthInteraction interaction)
        {
            items.put(interaction.id, interaction.copy());
        }

        @Override
        public synchronized QuantaOAuthInteraction find(String id)
        {
            QuantaOAuthInteraction item = items.get(id);
            return item == null ? null : item.copy();
        }

        @Override
        public synchronized String approve(String id, String userId, long nowMillis)
        {
            QuantaOAuthInteraction item = items.get(id);
            String gate = gate(item, nowMillis);
            if (gate != null)
            {
                return gate;
            }
            if ("approved".equals(item.status))
            {
                return userId.equals(item.subjectUserId) ? "same" : "conflict";
            }
            if (!"pending".equals(item.status))
            {
                return "conflict";
            }
            item.status = "approved";
            item.subjectUserId = userId;
            return "ok";
        }

        @Override
        public synchronized String deny(String id, long nowMillis)
        {
            QuantaOAuthInteraction item = items.get(id);
            String gate = gate(item, nowMillis);
            if (gate != null)
            {
                return gate;
            }
            item.status = "denied";
            item.consumed = true;
            return "ok";
        }

        @Override
        public synchronized String consume(String id, long nowMillis)
        {
            QuantaOAuthInteraction item = items.get(id);
            String gate = gate(item, nowMillis);
            if (gate != null)
            {
                return gate;
            }
            if (!"approved".equals(item.status))
            {
                return "pending";
            }
            item.consumed = true;
            return "ok";
        }

        private static String gate(QuantaOAuthInteraction item, long nowMillis)
        {
            if (item == null)
            {
                return "missing";
            }
            if (item.expiresAt <= nowMillis)
            {
                return "expired";
            }
            if (item.consumed)
            {
                return "consumed";
            }
            return null;
        }
    }
}

@Component
class RedisQuantaOAuthInteractionStore implements QuantaOAuthInteractionStore
{
    private static final String APPROVE = """
            local status = redis.call('HGET', KEYS[1], 'status')
            if not status then return 'missing' end
            local exp = tonumber(redis.call('HGET', KEYS[1], 'expiresAt'))
            if exp and exp <= tonumber(ARGV[2]) then return 'expired' end
            if redis.call('HGET', KEYS[1], 'consumed') == '1' then return 'consumed' end
            if status == 'approved' then
              if redis.call('HGET', KEYS[1], 'subjectUserId') == ARGV[1] then return 'same' end
              return 'conflict'
            end
            if status ~= 'pending' then return 'conflict' end
            redis.call('HSET', KEYS[1], 'status', 'approved', 'subjectUserId', ARGV[1])
            return 'ok'
            """;

    private static final String DENY = """
            local status = redis.call('HGET', KEYS[1], 'status')
            if not status then return 'missing' end
            local exp = tonumber(redis.call('HGET', KEYS[1], 'expiresAt'))
            if exp and exp <= tonumber(ARGV[1]) then return 'expired' end
            if redis.call('HGET', KEYS[1], 'consumed') == '1' then return 'consumed' end
            redis.call('HSET', KEYS[1], 'status', 'denied', 'consumed', '1')
            return 'ok'
            """;

    private static final String CONSUME = """
            local status = redis.call('HGET', KEYS[1], 'status')
            if not status then return 'missing' end
            local exp = tonumber(redis.call('HGET', KEYS[1], 'expiresAt'))
            if exp and exp <= tonumber(ARGV[1]) then return 'expired' end
            if redis.call('HGET', KEYS[1], 'consumed') == '1' then return 'consumed' end
            if status ~= 'approved' then return 'pending' end
            redis.call('HSET', KEYS[1], 'consumed', '1')
            return 'ok'
            """;

    private final StringRedisTemplate redis;

    RedisQuantaOAuthInteractionStore(StringRedisTemplate redis)
    {
        this.redis = redis;
    }

    @Override
    public void create(QuantaOAuthInteraction interaction)
    {
        Map<String, String> fields = new HashMap<>();
        fields.put("clientId", interaction.clientId);
        fields.put("redirectUri", interaction.redirectUri);
        fields.put("state", interaction.state);
        fields.put("nonce", interaction.nonce);
        fields.put("codeChallenge", interaction.codeChallenge);
        fields.put("browserId", interaction.browserId);
        fields.put("csrfToken", interaction.csrfToken);
        fields.put("status", interaction.status);
        fields.put("subjectUserId", interaction.subjectUserId == null ? "" : interaction.subjectUserId);
        fields.put("expiresAt", Long.toString(interaction.expiresAt));
        fields.put("consumed", "0");
        String key = key(interaction.id);
        redis.opsForHash().putAll(key, fields);
        redis.expire(key, java.time.Duration.ofMinutes(10));
    }

    @Override
    public QuantaOAuthInteraction find(String id)
    {
        Map<Object, Object> fields = redis.opsForHash().entries(key(id));
        if (fields == null || fields.isEmpty())
        {
            return null;
        }
        QuantaOAuthInteraction interaction = new QuantaOAuthInteraction();
        interaction.id = id;
        interaction.clientId = text(fields, "clientId");
        interaction.redirectUri = text(fields, "redirectUri");
        interaction.state = text(fields, "state");
        interaction.nonce = text(fields, "nonce");
        interaction.codeChallenge = text(fields, "codeChallenge");
        interaction.browserId = text(fields, "browserId");
        interaction.csrfToken = text(fields, "csrfToken");
        interaction.status = text(fields, "status");
        interaction.subjectUserId = text(fields, "subjectUserId");
        interaction.expiresAt = Long.parseLong(text(fields, "expiresAt"));
        interaction.consumed = "1".equals(text(fields, "consumed"));
        return interaction;
    }

    @Override
    public String approve(String id, String userId, long nowMillis)
    {
        return eval(APPROVE, id, userId, Long.toString(nowMillis));
    }

    @Override
    public String deny(String id, long nowMillis)
    {
        return eval(DENY, id, Long.toString(nowMillis));
    }

    @Override
    public String consume(String id, long nowMillis)
    {
        return eval(CONSUME, id, Long.toString(nowMillis));
    }

    private String eval(String scriptText, String id, String... args)
    {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setResultType(String.class);
        script.setScriptText(scriptText);
        String result = redis.execute(script, List.of(key(id)), (Object[]) args);
        return result == null ? "missing" : result;
    }

    private static String key(String id)
    {
        return "oauth:interaction:" + id;
    }

    private static String text(Map<Object, Object> fields, String name)
    {
        Object value = fields.get(name);
        return value == null ? "" : value.toString();
    }
}
