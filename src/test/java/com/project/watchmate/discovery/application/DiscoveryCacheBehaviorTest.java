package com.project.watchmate.discovery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.project.watchmate.common.cache.WatchMateCacheEvictionService;
import com.project.watchmate.common.mapper.WatchMateMapper;
import com.project.watchmate.discovery.domain.CuratedContent;
import com.project.watchmate.discovery.domain.CuratedContentCategory;
import com.project.watchmate.discovery.dto.DiscoveryMediaItemDTO;
import com.project.watchmate.discovery.persistence.CuratedContentRepository;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;

@SpringJUnitConfig(DiscoveryCacheBehaviorTest.TestConfig.class)
class DiscoveryCacheBehaviorTest {

    @Autowired
    private DiscoverService discoverService;

    @Autowired
    private CuratedContentRepository curatedContentRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private WatchMateCacheEvictionService cacheEvictionService;

    @BeforeEach
    void setUp() {
        reset(curatedContentRepository);
        cacheManager.getCacheNames().stream()
            .map(cacheManager::getCache)
            .filter(java.util.Objects::nonNull)
            .forEach(org.springframework.cache.Cache::clear);
    }

    @Test
    void curatedContentLists_whenSameBucketRepeated_hitsRepositoryOnce() {
        when(curatedContentRepository.findByCategoryKeyWithMediaOrderByRankPositionAsc(CuratedContentCategory.TRENDING_MOVIES))
            .thenReturn(List.of(curatedContent(100L, "Movie")));

        List<DiscoveryMediaItemDTO> first = discoverService.getTrendingMovies();
        List<DiscoveryMediaItemDTO> second = discoverService.getTrendingMovies();

        assertEquals("Movie", first.get(0).getTitle());
        assertEquals("Movie", second.get(0).getTitle());
        verify(curatedContentRepository, times(1))
            .findByCategoryKeyWithMediaOrderByRankPositionAsc(CuratedContentCategory.TRENDING_MOVIES);
    }

    @Test
    void discoveryEviction_whenCalled_clearsOnlyDiscoveryCaches() {
        when(curatedContentRepository.findByCategoryKeyWithMediaOrderByRankPositionAsc(CuratedContentCategory.TRENDING_MOVIES))
            .thenReturn(List.of(curatedContent(100L, "Movie")));

        discoverService.getTrendingMovies();
        cacheEvictionService.evictDiscoveryContentCaches();
        discoverService.getTrendingMovies();

        verify(curatedContentRepository, times(2))
            .findByCategoryKeyWithMediaOrderByRankPositionAsc(CuratedContentCategory.TRENDING_MOVIES);
    }

    private CuratedContent curatedContent(Long tmdbId, String title) {
        return CuratedContent.builder()
            .categoryKey(CuratedContentCategory.TRENDING_MOVIES)
            .media(Media.builder()
                .id(tmdbId)
                .tmdbId(tmdbId)
                .title(title)
                .type(MediaType.MOVIE)
                .genres(List.of())
                .build())
            .build();
    }

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        CuratedContentRepository curatedContentRepository() {
            return org.mockito.Mockito.mock(CuratedContentRepository.class);
        }

        @Bean
        WatchMateMapper watchMateMapper() {
            return new WatchMateMapper();
        }

        @Bean
        DiscoverService discoverService(CuratedContentRepository curatedContentRepository, WatchMateMapper watchMateMapper) {
            return new DiscoverService(curatedContentRepository, watchMateMapper);
        }

        @Bean
        WatchMateCacheEvictionService watchMateCacheEvictionService() {
            return new WatchMateCacheEvictionService();
        }
    }
}
