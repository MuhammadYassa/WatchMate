package com.project.watchmate.show.metadata.application;

import com.project.watchmate.media.tmdb.application.TmdbService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.show.catalog.application.ShowCatalogService;
import com.project.watchmate.show.metadata.dto.NextEpisodeAiringDTO;
import com.project.watchmate.show.metadata.dto.PublicShowEpisodeMetadataDTO;
import com.project.watchmate.show.metadata.dto.PublicShowMetadataDTO;
import com.project.watchmate.show.metadata.dto.PublicShowSeasonMetadataDTO;
import com.project.watchmate.show.metadata.dto.ShowDetailsDTO;
import com.project.watchmate.show.metadata.dto.ShowEpisodeDetailsDTO;
import com.project.watchmate.common.error.UserNotFoundException;
import com.project.watchmate.common.mapper.WatchMateMapper;
import com.project.watchmate.media.tmdb.dto.TmdbTvDetailsDTO;
import com.project.watchmate.show.metadata.mapper.ShowMetadataMapper;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.extras.application.MediaExtrasService;
import com.project.watchmate.media.extras.dto.MediaExtrasDTO;
import com.project.watchmate.review.domain.Review;
import com.project.watchmate.show.tracking.domain.UserShowTracking;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.review.persistence.ReviewRepository;
import com.project.watchmate.show.tracking.persistence.UserShowTrackingRepository;
import com.project.watchmate.user.persistence.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShowMetadataService {

    private final ShowCatalogService showCatalogService;

    private final TmdbService tmdbService;

    private final ShowMetadataMapper showMetadataMapper;

    private final WatchMateMapper watchMateMapper;

    private final UsersRepository usersRepository;

    private final ReviewRepository reviewRepository;

    private final UserShowTrackingRepository userShowTrackingRepository;

    private final PublicShowMetadataCacheService publicShowMetadataCacheService;

    private final MediaExtrasService mediaExtrasService;

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

        PublicShowMetadataDTO publicMetadata = publicShowMetadataCacheService.getShowMetadata(tmdbId);
        MediaExtrasDTO extras = mediaExtrasService.getExtras(tmdbId, MediaType.SHOW);
        return toShowDetailsDTO(publicMetadata, reviews, userContext.isFavourited(), userContext.watchStatus(), extras);
    }

    @Transactional
    public Page<ShowEpisodeDetailsDTO> getShowSeasonDetails(Long tmdbId, Integer seasonNumber, MediaType mediaType, Users user, int page, int size) {
        showCatalogService.validateShowType(mediaType);
        PublicShowSeasonMetadataDTO publicSeason = publicShowMetadataCacheService.getSeasonMetadata(tmdbId, seasonNumber);
        Media media = user == null ? null : showCatalogService.findImportedShow(tmdbId);
        Set<String> watchedKeys = resolveWatchedEpisodeKeys(user, media);

        List<ShowEpisodeDetailsDTO> allEpisodes = publicSeason.getEpisodes().stream()
            .map(ep -> toShowEpisodeDetailsDTO(ep, watchedKeys))
            .toList();

        int from = page * size;
        int to = Math.min(from + size, allEpisodes.size());
        List<ShowEpisodeDetailsDTO> content = from >= allEpisodes.size() ? List.of() : allEpisodes.subList(from, to);
        return new PageImpl<>(content, PageRequest.of(page, size), allEpisodes.size());
    }

    private UserContext resolveUserContext(Users userParam, Media media) {
        if (userParam == null) {
            return new UserContext(false, WatchStatus.NONE);
        }

        Long userId = java.util.Objects.requireNonNull(java.util.Objects.requireNonNull(userParam, "userParam").getId(), "userParam.id");
        Users user = usersRepository.findByIdWithFavorites(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        boolean isFavourited = media != null && user.getFavorites().contains(media);
        UserShowTracking tracking = media == null ? null : userShowTrackingRepository.findByUserAndMedia(user, media).orElse(null);
        WatchStatus watchStatus = tracking != null ? tracking.getStatus() : WatchStatus.NONE;

        return new UserContext(isFavourited, watchStatus);
    }

    private Set<String> resolveWatchedEpisodeKeys(Users user, Media media) {
        if (user == null || media == null) {
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

    private ShowDetailsDTO toShowDetailsDTO(
        PublicShowMetadataDTO publicMetadata,
        List<Review> reviews,
        Boolean isFavourited,
        WatchStatus watchStatus,
        MediaExtrasDTO extras
    ) {
        return ShowDetailsDTO.builder()
            .tmdbId(publicMetadata.getTmdbId())
            .type(publicMetadata.getType())
            .title(publicMetadata.getTitle())
            .overview(publicMetadata.getOverview())
            .posterPath(publicMetadata.getPosterPath())
            .backdropPath(publicMetadata.getBackdropPath())
            .firstAirDate(publicMetadata.getFirstAirDate())
            .rating(publicMetadata.getRating())
            .genres(publicMetadata.getGenres())
            .reviews(reviews.stream().map(watchMateMapper::mapToReviewDTO).toList())
            .numberOfSeasons(publicMetadata.getNumberOfSeasons())
            .numberOfEpisodes(publicMetadata.getNumberOfEpisodes())
            .lastAirDate(publicMetadata.getLastAirDate())
            .tmdbShowStatus(publicMetadata.getTmdbShowStatus())
            .nextEpisodeAirDate(publicMetadata.getNextEpisodeAirDate())
            .nextEpisodeSeasonNumber(publicMetadata.getNextEpisodeSeasonNumber())
            .nextEpisodeEpisodeNumber(publicMetadata.getNextEpisodeEpisodeNumber())
            .nextEpisodeName(publicMetadata.getNextEpisodeName())
            .lastEpisodeToAirSeasonNumber(publicMetadata.getLastEpisodeToAirSeasonNumber())
            .lastEpisodeToAirEpisodeNumber(publicMetadata.getLastEpisodeToAirEpisodeNumber())
            .lastEpisodeToAirName(publicMetadata.getLastEpisodeToAirName())
            .seasons(publicMetadata.getSeasons())
            .isFavourited(isFavourited)
            .watchStatus(watchStatus)
            .cast(extras.cast())
            .bestTrailer(extras.bestTrailer())
            .watchProviders(extras.watchProviders())
            .build();
    }

    private ShowEpisodeDetailsDTO toShowEpisodeDetailsDTO(PublicShowEpisodeMetadataDTO episode, Set<String> watchedEpisodeKeys) {
        return ShowEpisodeDetailsDTO.builder()
            .tmdbEpisodeId(episode.getTmdbEpisodeId())
            .seasonNumber(episode.getSeasonNumber())
            .episodeNumber(episode.getEpisodeNumber())
            .name(episode.getName())
            .overview(episode.getOverview())
            .airDate(episode.getAirDate())
            .runtime(episode.getRuntime())
            .stillPath(episode.getStillPath())
            .isAired(episode.getIsAired())
            .watched(watchedEpisodeKeys.contains(episode.getSeasonNumber() + ":" + episode.getEpisodeNumber()))
            .build();
    }
}






