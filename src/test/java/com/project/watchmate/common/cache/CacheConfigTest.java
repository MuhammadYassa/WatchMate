package com.project.watchmate.common.cache;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CacheConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(CacheConfig.class);

    @Test
    void cacheConfig_whenCacheDisabled_doesNotCreateCacheManager() {
        contextRunner
            .withPropertyValues("watchmate.cache.enabled=false")
            .run(context -> assertFalse(context.containsBean("cacheManager")));
    }
}
