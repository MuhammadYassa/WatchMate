package com.project.watchmate.Services;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.Dto.NextEpisodeAiringDTO;
import com.project.watchmate.Dto.ShowDetailsDTO;
import com.project.watchmate.Dto.ShowSeasonsDetailsDTO;
import com.project.watchmate.Dto.TmdbTvDetailsDTO;
import com.project.watchmate.Mappers.ShowMetadataMapper;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.Review;
import com.project.watchmate.Models.UserShowTracking;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.ReviewRepository;
import com.project.watchmate.Repositories.UserShowTrackingRepository;
import com.project.watchmate.Repositories.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShowMetadataService {

    private final ShowCatalogService showCatalogService;

    private final TmdbService tmdbService;

    private final ShowMetadataMapper showMetadataMapper;

    private final UsersRepository usersRepository;

    private final ReviewRepository reviewRepository;

    private final UserShowTrackingRepository userShowTrackingRepository;

    @Transactional
    public NextEpisodeAiringDTO getNextEpisode(Long tmdbId, MediaType mediaType) {
        showCatalogService.validateShowType(mediaType);
        TmdbTvDetailsDTO tvDetails = tmdbService.fetchTvDetails(tmdbId);

        Media importedShow = showCatalogService.findImportedShow(tmdbId);
        if (importedShow != null) {
            tmdbService.refreshShowSnapshot(importedShow, tvDetails);
        }

        return showMetadataMapper.mapToNextEpisodeDto(tmdbId, tvDetails);
    }

    @Transactional
    public ShowDetailsDTO getShowDetails(Long tmdbId, MediaType mediaType, Users userParam) {
        showCatalogService.validateShowType(mediaType);

        Media importedShow = showCatalogService.findImportedShow(tmdbId);
        List<Review> reviews = importedShow == null ? List.of() : reviewRepository.findByMedia(importedShow);
        UserContext userContext = resolveUserContext(userParam, importedShow);

        TmdbTvDetailsDTO tvDetails = tmdbService.fetchTvDetails(tmdbId);
        if (importedShow != null) {
            tmdbService.refreshShowSnapshot(importedShow, tvDetails);
        }

        return showMetadataMapper.mapToShowDetailsDTO(tvDetails, reviews, userContext.isFavourited(), userContext.watchStatus());
    }

    @Transactional
    public ShowSeasonsDetailsDTO getShowSeasonDetails(Long tmdbId, Integer seasonNumber, MediaType mediaType, Users user) {
        showCatalogService.validateShowType(mediaType);
        Media media = showCatalogService.ensureBasicShowImported(tmdbId);
        ShowCatalogService.CachedSeasonData cachedSeason = showCatalogService.ensureSeasonCached(media, tmdbId, seasonNumber);
        return showMetadataMapper.mapToShowSeasonDetails(tmdbId, cachedSeason.season(), cachedSeason.episodes(), resolveWatchedEpisodeKeys(user, media));
    }

    private UserContext resolveUserContext(Users userParam, Media media) {
        if (userParam == null) {
            return new UserContext(false, WatchStatus.NONE);
        }

        Long userId = java.util.Objects.requireNonNull(java.util.Objects.requireNonNull(userParam, "userParam").getId(), "userParam.id");
        Users user = usersRepository.findByIdWithFavorites(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isFavourited = media != null && user.getFavorites().contains(media);
        UserShowTracking tracking = media == null ? null : userShowTrackingRepository.findByUserAndMedia(user, media).orElse(null);
        WatchStatus watchStatus = tracking != null ? tracking.getStatus() : WatchStatus.NONE;

        return new UserContext(isFavourited, watchStatus);
    }

    private Set<String> resolveWatchedEpisodeKeys(Users user, Media media) {
        if (user == null) {
            return Set.of();
        }

        UserShowTracking tracking = userShowTrackingRepository.findWithEpisodeWatchesByUserAndMedia(user, media).orElse(null);
        if (tracking == null) {
            return Set.of();
        }

        return tracking.getEpisodeWatches().stream()
            .map(row -> row.getSeasonNumber() + ":" + row.getEpisodeNumber())
            .collect(Collectors.toSet());
    }

    private record UserContext(boolean isFavourited, WatchStatus watchStatus) {
    }
}
