package com.zeroq.sensor.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(List.of(
                "sensorById",
                "sensorsByPlace",
                "latestSnapshotByPlace"
        ));
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(Duration.ofMinutes(3))
        );
        return cacheManager;
    }
}
