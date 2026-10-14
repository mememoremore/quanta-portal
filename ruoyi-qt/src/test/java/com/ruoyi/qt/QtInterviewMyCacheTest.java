package com.ruoyi.qt;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import com.ruoyi.framework.config.RedisConfig;
import com.ruoyi.qt.domain.QtInterviewResult;
import com.ruoyi.qt.service.impl.QtInterviewServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QtInterviewMyCacheTest
{
    @Test
    void myViewCachesSkipNullResults() throws Exception
    {
        assertSkipsNull("selectMyApplication", Long.class);
        assertSkipsNull("selectMyProfile", Long.class);
        assertSkipsNull("selectMyResultList", Long.class);
    }

    @Test
    void cacheSerializerRoundTripsInterviewScore()
    {
        QtInterviewResult result = new QtInterviewResult();
        result.setDepartment("BACKEND");
        result.setScore(new BigDecimal("88.50"));
        ArrayList<QtInterviewResult> stored = new ArrayList<>();
        stored.add(result);

        GenericJackson2JsonRedisSerializer serializer = RedisConfig.cacheValueSerializer();
        @SuppressWarnings("unchecked")
        List<QtInterviewResult> restored = (List<QtInterviewResult>) serializer.deserialize(serializer.serialize(stored));

        assertNotNull(restored);
        assertEquals(1, restored.size());
        assertEquals("BACKEND", restored.get(0).getDepartment());
        assertEquals(0, new BigDecimal("88.50").compareTo(restored.get(0).getScore()));
    }

    private static void assertSkipsNull(String methodName, Class<?>... parameterTypes) throws Exception
    {
        Method method = QtInterviewServiceImpl.class.getMethod(methodName, parameterTypes);
        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        assertNotNull(cacheable, methodName + " should be @Cacheable");
        assertTrue(cacheable.unless().contains("#result == null"),
                methodName + " must skip caching null so unfinished applications do not throw");
    }
}
