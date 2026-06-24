package com.project.watchmate.media.extras.application;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.extras.dto.CastMemberDTO;
import com.project.watchmate.media.extras.dto.MediaExtrasDTO;
import com.project.watchmate.media.extras.dto.TrailerDTO;
import com.project.watchmate.media.extras.dto.WatchProviderEntryDTO;
import com.project.watchmate.media.extras.dto.WatchProvidersDTO;
import com.project.watchmate.media.tmdb.client.TmdbClient;
import com.project.watchmate.media.tmdb.dto.TmdbCastMemberDTO;
import com.project.watchmate.media.tmdb.dto.TmdbCreditsDTO;
import com.project.watchmate.media.tmdb.dto.TmdbVideoDTO;
import com.project.watchmate.media.tmdb.dto.TmdbVideosResponseDTO;
import com.project.watchmate.media.tmdb.dto.TmdbWatchProviderEntryDTO;
import com.project.watchmate.media.tmdb.dto.TmdbWatchProviderRegionDTO;
import com.project.watchmate.media.tmdb.dto.TmdbWatchProvidersResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaExtrasService {

    private static final int DEFAULT_CAST_LIMIT = 10;
    private static final String DEFAULT_REGION = "US";
    private static final String YOUTUBE_SITE = "YouTube";
    private static final String TRAILER_TYPE = "Trailer";
    private static final String TEASER_TYPE = "Teaser";

    private final TmdbClient tmdbClient;

    @Value("${watchmate.tmdb.default-region:US}")
    private String defaultRegion;

    @Value("${watchmate.tmdb.cast-limit:10}")
    private Integer castLimit;

    public MediaExtrasDTO getExtras(Long tmdbId, MediaType type) {
        String region = normalizeRegion(defaultRegion);
        return new MediaExtrasDTO(
            fetchCast(tmdbId, type),
            fetchBestTrailer(tmdbId, type),
            fetchWatchProviders(tmdbId, type, region)
        );
    }

    private List<CastMemberDTO> fetchCast(Long tmdbId, MediaType type) {
        try {
            return mapCast(tmdbClient.fetchCredits(tmdbId, type));
        } catch (RuntimeException ex) {
            log.warn("Failed to fetch media cast tmdbId={} type={}", tmdbId, type, ex);
            return List.of();
        }
    }

    private TrailerDTO fetchBestTrailer(Long tmdbId, MediaType type) {
        try {
            return selectBestTrailer(tmdbClient.fetchVideos(tmdbId, type));
        } catch (RuntimeException ex) {
            log.warn("Failed to fetch media videos tmdbId={} type={}", tmdbId, type, ex);
            return null;
        }
    }

    private WatchProvidersDTO fetchWatchProviders(Long tmdbId, MediaType type, String region) {
        try {
            return mapWatchProviders(tmdbClient.fetchWatchProviders(tmdbId, type), region);
        } catch (RuntimeException ex) {
            log.warn("Failed to fetch media watch providers tmdbId={} type={} region={}", tmdbId, type, region, ex);
            return emptyWatchProviders(region);
        }
    }

    private List<CastMemberDTO> mapCast(TmdbCreditsDTO credits) {
        if (credits == null || credits.getCast() == null) {
            return List.of();
        }

        int limit = castLimit == null || castLimit <= 0 ? DEFAULT_CAST_LIMIT : castLimit;
        return credits.getCast().stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(TmdbCastMemberDTO::getOrder, Comparator.nullsLast(Integer::compareTo)))
            .limit(limit)
            .map(member -> CastMemberDTO.builder()
                .tmdbPersonId(member.getId())
                .name(member.getName())
                .character(member.getCharacter())
                .profilePath(member.getProfilePath())
                .order(member.getOrder())
                .knownForDepartment(member.getKnownForDepartment())
                .build())
            .toList();
    }

    private TrailerDTO selectBestTrailer(TmdbVideosResponseDTO response) {
        if (response == null || response.getResults() == null) {
            return null;
        }

        return response.getResults().stream()
            .filter(this::isYoutubeVideo)
            .min(Comparator
                .comparingInt(this::trailerPriority)
                .thenComparing(this::publishedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(this::toTrailerDTO)
            .orElse(null);
    }

    private WatchProvidersDTO mapWatchProviders(TmdbWatchProvidersResponseDTO response, String region) {
        if (response == null || response.getResults() == null) {
            return emptyWatchProviders(region);
        }

        TmdbWatchProviderRegionDTO regionProviders = findRegion(response.getResults(), region);
        if (regionProviders == null) {
            return emptyWatchProviders(region);
        }

        return WatchProvidersDTO.builder()
            .region(region)
            .link(regionProviders.getLink())
            .flatrate(mapProviderEntries(regionProviders.getFlatrate()))
            .rent(mapProviderEntries(regionProviders.getRent()))
            .buy(mapProviderEntries(regionProviders.getBuy()))
            .ads(mapProviderEntries(regionProviders.getAds()))
            .free(mapProviderEntries(regionProviders.getFree()))
            .build();
    }

    private TmdbWatchProviderRegionDTO findRegion(Map<String, TmdbWatchProviderRegionDTO> results, String region) {
        TmdbWatchProviderRegionDTO exact = results.get(region);
        if (exact != null) {
            return exact;
        }
        return results.entrySet().stream()
            .filter(entry -> entry.getKey() != null && entry.getKey().equalsIgnoreCase(region))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }

    private List<WatchProviderEntryDTO> mapProviderEntries(List<TmdbWatchProviderEntryDTO> entries) {
        if (entries == null) {
            return List.of();
        }

        return entries.stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(TmdbWatchProviderEntryDTO::getDisplayPriority, Comparator.nullsLast(Integer::compareTo)))
            .map(entry -> WatchProviderEntryDTO.builder()
                .providerId(entry.getProviderId())
                .providerName(entry.getProviderName())
                .logoPath(entry.getLogoPath())
                .displayPriority(entry.getDisplayPriority())
                .build())
            .toList();
    }

    private boolean isYoutubeVideo(TmdbVideoDTO video) {
        return video != null
            && video.getKey() != null
            && !video.getKey().isBlank()
            && YOUTUBE_SITE.equalsIgnoreCase(video.getSite());
    }

    private int trailerPriority(TmdbVideoDTO video) {
        boolean official = Boolean.TRUE.equals(video.getOfficial());
        boolean trailer = TRAILER_TYPE.equalsIgnoreCase(video.getType());
        boolean teaser = TEASER_TYPE.equalsIgnoreCase(video.getType());

        if (official && trailer) {
            return 0;
        }
        if (official) {
            return 1;
        }
        if (trailer) {
            return 2;
        }
        if (teaser) {
            return 3;
        }
        return 4;
    }

    private OffsetDateTime publishedAt(TmdbVideoDTO video) {
        if (video.getPublishedAt() == null || video.getPublishedAt().isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(video.getPublishedAt());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private TrailerDTO toTrailerDTO(TmdbVideoDTO video) {
        String key = video.getKey();
        return TrailerDTO.builder()
            .key(key)
            .name(video.getName())
            .site(video.getSite())
            .type(video.getType())
            .official(video.getOfficial())
            .publishedAt(video.getPublishedAt())
            .youtubeUrl("https://www.youtube.com/watch?v=" + key)
            .thumbnailUrl("https://img.youtube.com/vi/" + key + "/hqdefault.jpg")
            .build();
    }

    private WatchProvidersDTO emptyWatchProviders(String region) {
        return WatchProvidersDTO.builder()
            .region(region)
            .flatrate(List.of())
            .rent(List.of())
            .buy(List.of())
            .ads(List.of())
            .free(List.of())
            .build();
    }

    private String normalizeRegion(String region) {
        if (region == null || region.isBlank()) {
            return DEFAULT_REGION;
        }
        return region.trim().toUpperCase(Locale.ROOT);
    }
}
