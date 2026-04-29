package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.Dto.DiscoveryMediaItemDTO;
import com.project.watchmate.Mappers.WatchMateMapper;
import com.project.watchmate.Models.CuratedContent;
import com.project.watchmate.Models.CuratedContentCategory;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Repositories.CuratedContentRepository;

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
