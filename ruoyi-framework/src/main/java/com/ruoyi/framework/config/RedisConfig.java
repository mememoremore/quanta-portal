package com.ruoyi.framework.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.ruoyi.common.constant.CacheConstants;

/**
 * redis配置
 * 
 * @author ruoyi
 */
@SuppressWarnings("deprecation")
@Configuration
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport
{
    /**
     * 业务缓存 TTL。凡是不在此表中的 cacheName 走 {@link CacheConstants#CACHE_DEFAULT_TTL_MINUTES} 分钟。
     * <p>
     * 注意：走 {@code startPage()} 分页的查询结果禁止登记在此并配 @Cacheable —— PageHelper 返回的是
     * {@code com.github.pagehelper.Page}，反序列化后会退化成普通 List，导致
     * {@code new PageInfo(list).getTotal()} 变成当页条数，前端分页控件随之失效。
     */
    private static final Map<String, Duration> CACHE_TTL = new HashMap<>();

    static
    {
        CACHE_TTL.put(CacheConstants.CACHE_QT_DASHBOARD_STATS, Duration.ofSeconds(CacheConstants.TTL_QT_DASHBOARD_STATS));
        CACHE_TTL.put(CacheConstants.CACHE_QT_RECRUIT_STATISTICS,
                Duration.ofSeconds(CacheConstants.TTL_QT_RECRUIT_STATISTICS));
        CACHE_TTL.put(CacheConstants.CACHE_QT_ACTIVITY_LIST, Duration.ofSeconds(CacheConstants.TTL_QT_ACTIVITY_LIST));
        CACHE_TTL.put(CacheConstants.CACHE_QT_ITEM_LIST, Duration.ofSeconds(CacheConstants.TTL_QT_ITEM_LIST));
        CACHE_TTL.put(CacheConstants.CACHE_QT_WORKSTATION_LIST,
                Duration.ofSeconds(CacheConstants.TTL_QT_WORKSTATION_LIST));
        CACHE_TTL.put(CacheConstants.CACHE_QT_BOOK_LIST, Duration.ofSeconds(CacheConstants.TTL_QT_BOOK_LIST));
        CACHE_TTL.put(CacheConstants.CACHE_QT_MEMBER_COHORTS, Duration.ofSeconds(CacheConstants.TTL_QT_MEMBER_COHORTS));
        CACHE_TTL.put(CacheConstants.CACHE_QT_PAYMENT_CONFIG, Duration.ofSeconds(CacheConstants.TTL_QT_PAYMENT_CONFIG));
        CACHE_TTL.put(CacheConstants.CACHE_QT_ACTIVITY_DETAIL, Duration.ofSeconds(CacheConstants.TTL_QT_ACTIVITY_DETAIL));
        CACHE_TTL.put(CacheConstants.CACHE_QT_BOOK_DETAIL, Duration.ofSeconds(CacheConstants.TTL_QT_BOOK_DETAIL));
        CACHE_TTL.put(CacheConstants.CACHE_QT_MY_APPLICATION, Duration.ofSeconds(CacheConstants.TTL_QT_MY_VIEW));
        CACHE_TTL.put(CacheConstants.CACHE_QT_MY_PROFILE, Duration.ofSeconds(CacheConstants.TTL_QT_MY_VIEW));
        CACHE_TTL.put(CacheConstants.CACHE_QT_MY_RESULTS, Duration.ofSeconds(CacheConstants.TTL_QT_MY_VIEW));
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory)
    {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(CacheConstants.CACHE_DEFAULT_TTL_MINUTES))
                .prefixCacheNameWith(CacheConstants.CACHE_KEY_PREFIX)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(cacheValueSerializer()))
                // 空结果不缓存，避免把「查不到」也固化成一次数据不一致
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> initial = new HashMap<>();
        for (Map.Entry<String, Duration> entry : CACHE_TTL.entrySet())
        {
            initial.put(entry.getKey(), defaults.entryTtl(entry.getValue()));
        }

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(initial)
                .transactionAware()
                .build();
    }

    /**
     * 缓存值序列化器：带类型信息以便还原实体/集合，但只放行若依自身与 JDK 集合/数值包，
     * 防止反序列化 gadget。
     */
    public static GenericJackson2JsonRedisSerializer cacheValueSerializer()
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.ANY);
        BasicPolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.ruoyi.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.math.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.sql.")
                .build();
        mapper.activateDefaultTyping(validator, ObjectMapper.DefaultTyping.NON_FINAL, As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Bean
    @SuppressWarnings(value = { "unchecked", "rawtypes" })
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory)
    {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        FastJson2JsonRedisSerializer serializer = new FastJson2JsonRedisSerializer(Object.class);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        // Hash的key也采用StringRedisSerializer的序列化方式
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public DefaultRedisScript<Long> limitScript()
    {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(limitScriptText());
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    /**
     * 限流脚本
     */
    private String limitScriptText()
    {
        return "local key = KEYS[1]\n" +
                "local count = tonumber(ARGV[1])\n" +
                "local time = tonumber(ARGV[2])\n" +
                "local current = redis.call('get', key);\n" +
                "if current and tonumber(current) > count then\n" +
                "    return tonumber(current);\n" +
                "end\n" +
                "current = redis.call('incr', key)\n" +
                "if tonumber(current) == 1 then\n" +
                "    redis.call('expire', key, time)\n" +
                "end\n" +
                "return tonumber(current);";
    }
}
