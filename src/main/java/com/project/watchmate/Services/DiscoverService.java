package com.project.watchmate.Services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.Dto.DiscoveryMediaItemDTO;
import com.project.watchmate.Mappers.WatchMateMapper;
import com.project.watchmate.Models.CuratedContentCategory;
import com.project.watchmate.Repositories.CuratedContentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DiscoverService {

    private final CuratedContentRepository curatedContentRepository;

    private final WatchMateMapper watchMateMapper;

    @Transactional(readOnly = true)
    public List<DiscoveryMediaItemDTO> getTrendingMovies() {
        return getBucket(CuratedContentCategory.TRENDING_MOVIES);
    }

    @Transactional(readOnly = true)
    public List<DiscoveryMediaItemDTO> getTrendingShows() {
        return getBucket(CuratedContentCategory.TRENDING_SHOWS);
    }

    @Transactional(readOnly = true)
    public List<DiscoveryMediaItemDTO> getPopularNow() {
        return getBucket(CuratedContentCategory.POPULAR_NOW);
    }

    @Transactional(readOnly = true)
    public List<DiscoveryMediaItemDTO> getAiringToday() {
        return getBucket(CuratedContentCategory.AIRING_TODAY);
    }

    @Transactional(readOnly = true)
    public List<DiscoveryMediaItemDTO> getUpcoming() {
        return getBucket(CuratedContentCategory.UPCOMING);
    }

    @Transactional(readOnly = true)
    public List<DiscoveryMediaItemDTO> getRecommendedLater() {
        return getBucket(CuratedContentCategory.RECOMMENDED_LATER);
    }

    @Transactional(readOnly = true)
    public List<DiscoveryMediaItemDTO> getBucket(CuratedContentCategory category) {
        return curatedContentRepository.findByCategoryKeyWithMediaOrderByRankPositionAsc(category).stream()
            .map(curatedContent -> watchMateMapper.mapToDiscoveryMediaItemDTO(curatedContent.getMedia()))
            .toList();
    }
}
