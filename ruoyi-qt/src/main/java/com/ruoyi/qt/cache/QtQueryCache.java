package com.ruoyi.qt.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.redis.RedisCache;

/**
 * 查询结果缓存（分页列表专用）。
 * <p>
 * 之所以不用 {@code @Cacheable}：
 * <ol>
 * <li>分页接口的 {@code @PreAuthorize} 与缓存切面的先后顺序不可控，缓存可能先于鉴权返回结果；</li>
 * <li>{@code PageHelper.startPage()} 设在调用链里，缓存命中时若不执行查询，分页 ThreadLocal 会残留并污染
 * 同一线程上的下一个查询；</li>
 * <li>PageHelper 返回的 {@code Page} 是 {@code com.github} 下的类型，反序列化后会退化成普通 List，
 * 使 {@code total} 变成当页条数，前端分页控件随之失真。</li>
 * </ol>
 * 因此这里缓存「已经组装好的完整响应体」（含 total），并且只在真正要查库时才执行 loader。
 * 缓存值以 fastjson2 显式序列化成 JSON 字符串、按目标类反序列化，不依赖 autoType 白名单。
 *
 * @author Quanta
 */
@Component
public class QtQueryCache
{
    private static final Logger log = LoggerFactory.getLogger(QtQueryCache.class);

    /** 业务缓存统一键前缀，与若依自身手写缓存区隔 */
    public static final String PREFIX = "cache:";

    private final RedisCache redisCache;

    @Value("${qt.cache.enabled:true}")
    private boolean enabled;

    public QtQueryCache(RedisCache redisCache)
    {
        this.redisCache = redisCache;
    }

    /**
     * 分页列表专用：缓存整份 {@link TableDataInfo}（含 total），命中时按行类型还原成实体，
     * 使 Jackson 的序列化结果与未命中路径完全一致（否则 rows 会变成通用 Map，
     * 多出 {@code params:{}} 之类的结构差异）。
     *
     * @param cacheName 缓存名（同时作为清理前缀）
     * @param fingerprint 查询指纹
     * @param ttlSeconds 过期秒数
     * @param rowType 列表元素类型
     * @param loader 真正的查询逻辑
     */
    public TableDataInfo loadPage(String cacheName, String fingerprint, int ttlSeconds, Class<?> rowType,
            Supplier<TableDataInfo> loader)
    {
        if (!enabled)
        {
            return loader.get();
        }
        String key = key(cacheName, fingerprint);
        try
        {
            String json = redisCache.getCacheObject(key);
            if (json != null && !json.isEmpty())
            {
                JSONObject cached = JSON.parseObject(json);
                TableDataInfo hit = new TableDataInfo();
                hit.setCode(cached.getIntValue("code", HttpStatus.SUCCESS));
                hit.setMsg(cached.getString("msg"));
                hit.setTotal(cached.getLongValue("total"));
                JSONArray rows = cached.getJSONArray("rows");
                hit.setRows(rows == null ? new ArrayList<Object>() : rows.toJavaList(rowType));
                return hit;
            }
        }
        catch (Exception e)
        {
            // 缓存只是加速手段，读失败必须回源而不是把请求打挂
            log.warn("读取查询缓存失败，回源查询。key={}, err={}", key, e.getMessage());
        }
        TableDataInfo loaded = loader.get();
        if (loaded != null)
        {
            try
            {
                // WriteMapNullValue：保留 null 字段，避免命中缓存后响应体比未命中时少几个 key
                redisCache.setCacheObject(key,
                        JSON.toJSONString(loaded, JSONWriter.Feature.WriteMapNullValue), ttlSeconds, TimeUnit.SECONDS);
            }
            catch (Exception e)
            {
                log.warn("写入查询缓存失败，已忽略。key={}, err={}", key, e.getMessage());
            }
        }
        return loaded;
    }

    /**
     * 按缓存名整片失效（写操作后调用）。
     */
    public void evict(String cacheName)
    {
        try
        {
            Collection<String> keys = redisCache.keys(PREFIX + cacheName + "*");
            if (keys != null && !keys.isEmpty())
            {
                redisCache.deleteObject(keys);
            }
        }
        catch (Exception e)
        {
            log.warn("清理查询缓存失败，cacheName={}, err={}", cacheName, e.getMessage());
        }
    }

    /**
     * 失效若干精确键（用于按用户维度存放的缓存）。
     */
    public void evictKeys(String... keys)
    {
        try
        {
            for (String key : keys)
            {
                redisCache.deleteObject(key);
            }
        }
        catch (Exception e)
        {
            log.warn("清理缓存键失败，err={}", e.getMessage());
        }
    }

    public static String key(String cacheName, String fingerprint)
    {
        return PREFIX + cacheName + ":" + fingerprint;
    }

    public boolean isEnabled()
    {
        return enabled;
    }
}
