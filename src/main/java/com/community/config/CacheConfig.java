package com.community.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存降级：Redis 不可用时记录告警并回落数据库，不影响主流程。
 * <p>缓存是性能优化手段，不应成为单点故障。限流切面同样采用「降级放行」策略。</p>
 */
@Slf4j
@Configuration
public class CacheConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("缓存读取失败，已回落数据库 cache={} key={} cause={}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("缓存写入失败 cache={} key={} cause={}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("缓存清除失败 cache={} key={} cause={}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("缓存清空失败 cache={} cause={}", cache.getName(), exception.getMessage());
            }
        };
    }
}
