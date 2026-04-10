package org.wadhome.oag.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String THUMBNAIL_CACHE = "thumbnails";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(THUMBNAIL_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumWeight(100 * 1024 * 1024L) // 100MB
            .weigher((Object key, Object value) -> value instanceof byte[] b ? b.length : 1)
            .expireAfterAccess(Duration.ofMinutes(10)));
        return manager;
    }
}
