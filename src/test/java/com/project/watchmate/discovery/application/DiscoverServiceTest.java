package com.project.watchmate.discovery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.discovery.dto.DiscoveryMediaItemDTO;
import com.project.watchmate.common.mapper.WatchMateMapper;
import com.project.watchmate.discovery.domain.CuratedContent;
import com.project.watchmate.discovery.domain.CuratedContentCategory;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.discovery.persistence.CuratedContentRepository;

@ExtendWith(MockitoExtension.class)
class DiscoverServiceTest {

    @Mock
    private CuratedContentRepository curatedContentRepository;

    @Mock
    private WatchMateMapper watchMateMapper;

    @InjectMocks
    private DiscoverService discoverService;

    @Test
    void getTrendingMovies_ReturnsBucketInStoredOrder() {
        Media media = Media.builder()
            .id(1L)
            .tmdbId(100L)
            .title("Trending")
            .type(MediaType.MOVIE)
            .releaseDate(LocalDate.of(2024, 1, 1))
            .build();
        CuratedContent curatedContent = CuratedContent.builder()
            .categoryKey(CuratedContentCategory.TRENDING_MOVIES)
            .media(media)
            .mediaType(MediaType.MOVIE)
            .rankPosition(1)
            .build();
        DiscoveryMediaItemDTO dto = DiscoveryMediaItemDTO.builder().tmdbId(100L).title("Trending").type(MediaType.MOVIE).build();

        when(curatedContentRepository.findByCategoryKeyWithMediaOrderByRankPositionAsc(CuratedContentCategory.TRENDING_MOVIES))
            .thenReturn(List.of(curatedContent));
        when(watchMateMapper.mapToDiscoveryMediaItemDTO(media)).thenReturn(dto);

        List<DiscoveryMediaItemDTO> result = discoverService.getTrendingMovies();

        assertEquals(1, result.size());
        assertEquals("Trending", result.get(0).getTitle());
    }
}




