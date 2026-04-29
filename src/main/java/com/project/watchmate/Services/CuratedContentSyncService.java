package com.project.watchmate.Services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.project.watchmate.Clients.TmdbClient;
import com.project.watchmate.Dto.TmdbGenreDTO;
import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Models.ContentSyncResult;
import com.project.watchmate.Models.ContentSyncStatus;
import com.project.watchmate.Models.CuratedContent;
import com.project.watchmate.Models.CuratedContentCategory;
import com.project.watchmate.Models.Genre;
import com.project.watchmate.Models.GenreLookup;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.PopularMedia;
import com.project.watchmate.Repositories.ContentSyncStatusRepository;
import com.project.watchmate.Repositories.CuratedContentRepository;
import com.project.watchmate.Repositories.GenreLookupRepository;
import com.project.watchmate.Repositories.GenreRepository;
import com.project.watchmate.Repositories.PopularMediaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CuratedContentSyncService {

    public static final String STATUS_KEY = "DISCOVERY_SYNC";

    private static final int BUCKET_LIMIT = 20;

    private final TmdbClient tmdbClient;

    private final TmdbService tmdbService;

    private final CuratedContentRepository curatedContentRepository;

    private final GenreLookupRepository genreLookupRepository;

    private final GenreRepository genreRepository;

    private final PopularMediaRepository popularMediaRepository;

    private final ContentSyncStatusRepository contentSyncStatusRepository;

    private final PlatformTransactionManager transactionManager;

    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

    public boolean hasCachedContent() {
        return curatedContentRepository.count() > 0;
    }

    public void syncDiscoveryContent(String trigger) {
        if (!syncInProgress.compareAndSet(false, true)) {
            log.warn("Discovery content sync skipped trigger={} reason=already_running", trigger);
            return;
        }

        LocalDateTime attemptedAt = LocalDateTime.now();
        ContentSyncStatus syncStatus = getOrCreateStatus();
        syncStatus.setLastAttemptedAt(attemptedAt);
        contentSyncStatusRepository.save(syncStatus);

        try {
            FetchedDiscoveryData fetchedData = fetchDiscoveryData();
            new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                syncGenres(fetchedData.movieGenres(), fetchedData.showGenres(), attemptedAt));

            StoredDiscoveryData storedDiscoveryData = storeMediaAndBuildBuckets(fetchedData, attemptedAt);
            new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                persistSyncedContent(storedDiscoveryData, syncStatus, attemptedAt));

            log.info("Discovery content sync completed trigger={} buckets={}",
                trigger,
                storedDiscoveryData.bucketCounts());
        } catch (RuntimeException ex) {
            markSyncFailure(syncStatus, attemptedAt, ex);
            log.error("Discovery content sync failed trigger={}", trigger, ex);
        } finally {
            syncInProgress.set(false);
        }
    }

    private FetchedDiscoveryData fetchDiscoveryData() {
        return new FetchedDiscoveryData(
            tmdbClient.fetchGenres("movie"),
            tmdbClient.fetchGenres("tv"),
            tmdbClient.fetchTrending("movie"),
            tmdbClient.fetchTrending("tv"),
            tmdbClient.fetchPopular("movie"),
            tmdbClient.fetchPopular("tv"),
            tmdbClient.fetchAiringToday(),
            tmdbClient.fetchUpcomingMovies(),
            tmdbClient.fetchOnTheAir()
        );
    }

    protected void syncGenres(List<TmdbGenreDTO> movieGenres, List<TmdbGenreDTO> showGenres, LocalDateTime syncedAt) {
        upsertGlobalGenres(movieGenres);
        upsertGlobalGenres(showGenres);
        replaceGenreLookup(movieGenres, MediaType.MOVIE, syncedAt);
        replaceGenreLookup(showGenres, MediaType.SHOW, syncedAt);
    }

    private void upsertGlobalGenres(List<TmdbGenreDTO> tmdbGenres) {
        for (TmdbGenreDTO tmdbGenre : tmdbGenres) {
            Genre genre = genreRepository.findById(tmdbGenre.getId())
                .orElse(Genre.builder().id(tmdbGenre.getId()).build());
            genre.setName(tmdbGenre.getName());
            genreRepository.save(genre);
        }
    }

    private void replaceGenreLookup(List<TmdbGenreDTO> tmdbGenres, MediaType mediaType, LocalDateTime syncedAt) {
        genreLookupRepository.deleteByMediaType(mediaType);
        genreLookupRepository.saveAll(
            tmdbGenres.stream()
                .map(genre -> GenreLookup.builder()
                    .tmdbGenreId(genre.getId())
                    .name(genre.getName())
                    .mediaType(mediaType)
                    .syncedAt(syncedAt)
                    .build())
                .toList()
        );
    }

    private StoredDiscoveryData storeMediaAndBuildBuckets(FetchedDiscoveryData fetchedData, LocalDateTime syncedAt) {
        List<Media> trendingMovies = limit(tmdbService.upsertMediaFromTmdb(fetchedData.trendingMovies(), MediaType.MOVIE));
        List<Media> trendingShows = limit(tmdbService.upsertMediaFromTmdb(fetchedData.trendingShows(), MediaType.SHOW));
        List<Media> popularMovies = tmdbService.upsertMediaFromTmdb(fetchedData.popularMovies(), MediaType.MOVIE);
        List<Media> popularShows = tmdbService.upsertMediaFromTmdb(fetchedData.popularShows(), MediaType.SHOW);
        List<Media> airingToday = limit(tmdbService.upsertMediaFromTmdb(fetchedData.airingToday(), MediaType.SHOW));
        List<Media> upcomingMovies = tmdbService.upsertMediaFromTmdb(fetchedData.upcomingMovies(), MediaType.MOVIE);
        List<Media> onTheAir = tmdbService.upsertMediaFromTmdb(fetchedData.onTheAir(), MediaType.SHOW);

        List<Media> popularNow = interleave(popularMovies, popularShows, BUCKET_LIMIT);
        List<Media> upcoming = interleave(upcomingMovies, onTheAir, BUCKET_LIMIT);

        Map<CuratedContentCategory, List<Media>> buckets = new EnumMap<>(CuratedContentCategory.class);
        buckets.put(CuratedContentCategory.TRENDING_MOVIES, trendingMovies);
        buckets.put(CuratedContentCategory.TRENDING_SHOWS, trendingShows);
        buckets.put(CuratedContentCategory.POPULAR_NOW, popularNow);
        buckets.put(CuratedContentCategory.AIRING_TODAY, airingToday);
        buckets.put(CuratedContentCategory.UPCOMING, upcoming);

        Set<Long> alreadyUsedMediaIds = buckets.values().stream()
            .flatMap(List::stream)
            .map(Media::getId)
            .collect(Collectors.toSet());

        AllMediaLists allMedia = new AllMediaLists(popularMovies, popularShows, upcomingMovies, onTheAir, trendingMovies, trendingShows, airingToday);
        buckets.put(CuratedContentCategory.RECOMMENDED_LATER, buildRecommendedLater(alreadyUsedMediaIds, allMedia));

        return new StoredDiscoveryData(buckets, bucketCounts(buckets), syncedAt);
    }

    private List<Media> buildRecommendedLater(Set<Long> alreadyUsedMediaIds, AllMediaLists allMedia) {
        Map<Long, Media> distinctCandidates = new LinkedHashMap<>();
        List.of(allMedia.popularMovies(), allMedia.popularShows(), allMedia.upcomingMovies(), allMedia.onTheAir(), allMedia.trendingMovies(), allMedia.trendingShows(), allMedia.airingToday()).stream()
            .flatMap(List::stream)
            .filter(media -> media.getId() != null)
            .filter(media -> !alreadyUsedMediaIds.contains(media.getId()))
            .filter(media -> media.getRating() != null && media.getRating() >= 7.0d)
            .forEach(media -> distinctCandidates.putIfAbsent(media.getId(), media));

        return distinctCandidates.values().stream()
            .sorted(Comparator.comparingDouble(Media::getRating).reversed()
            .thenComparing(Comparator.comparing(Media::getReleaseDate,
            Comparator.nullsFirst(Comparator.reverseOrder()))
                )
            .thenComparing(Comparator.comparingLong(Media::getId).reversed()))
            .limit(BUCKET_LIMIT)
            .toList();
    }

    private List<Media> interleave(List<Media> first, List<Media> second, int limit) {
        List<Media> interleaved = new ArrayList<>();
        int index = 0;

        while (interleaved.size() < limit && (index < first.size() || index < second.size())) {
            if (index < first.size()) {
                interleaved.add(first.get(index));
            }
            if (interleaved.size() >= limit) {
                break;
            }
            if (index < second.size()) {
                interleaved.add(second.get(index));
            }
            index++;
        }

        return interleaved;
    }

    private List<Media> limit(List<Media> media) {
        return media.stream()
            .limit(BUCKET_LIMIT)
            .toList();
    }

    private BucketCounts bucketCounts(Map<CuratedContentCategory, List<Media>> buckets) {
        return new BucketCounts(
            sizeOf(buckets, CuratedContentCategory.TRENDING_MOVIES),
            sizeOf(buckets, CuratedContentCategory.TRENDING_SHOWS),
            sizeOf(buckets, CuratedContentCategory.POPULAR_NOW),
            sizeOf(buckets, CuratedContentCategory.AIRING_TODAY),
            sizeOf(buckets, CuratedContentCategory.UPCOMING),
            sizeOf(buckets, CuratedContentCategory.RECOMMENDED_LATER)
        );
    }

    private int sizeOf(Map<CuratedContentCategory, List<Media>> buckets, CuratedContentCategory category) {
        return buckets.getOrDefault(category, List.of()).size();
    }

    protected void persistSyncedContent(StoredDiscoveryData storedDiscoveryData, ContentSyncStatus syncStatus, LocalDateTime syncedAt) {
        for (Map.Entry<CuratedContentCategory, List<Media>> bucket : storedDiscoveryData.bucketMedia().entrySet()) {
            replaceBucket(bucket.getKey(), bucket.getValue(), syncedAt);
        }

        mirrorPopularNow(storedDiscoveryData.bucketMedia().getOrDefault(CuratedContentCategory.POPULAR_NOW, List.of()));
        markSyncSuccess(syncStatus, storedDiscoveryData.bucketCounts(), syncedAt);
    }

    private void replaceBucket(CuratedContentCategory category, List<Media> mediaItems, LocalDateTime syncedAt) {
        curatedContentRepository.deleteByCategoryKey(category);
        curatedContentRepository.saveAll(
            IntStream.range(0, mediaItems.size())
                .mapToObj(index -> CuratedContent.builder()
                    .categoryKey(category)
                    .media(mediaItems.get(index))
                    .mediaType(mediaItems.get(index).getType())
                    .rankPosition(index + 1)
                    .syncedAt(syncedAt)
                    .build())
                .toList()
        );
    }

    private void mirrorPopularNow(List<Media> popularNow) {
        List<Media> uniquePopularNow = distinctById(popularNow);

        popularMediaRepository.deleteAllInBatch();
        popularMediaRepository.flush();
        popularMediaRepository.saveAll(
            IntStream.range(0, uniquePopularNow.size())
                .mapToObj(index -> PopularMedia.builder()
                    .media(uniquePopularNow.get(index))
                    .popularityRank(index + 1)
                    .build())
                .toList()
        );
    }

    private List<Media> distinctById(List<Media> mediaItems) {
        Map<Long, Media> distinctMedia = new LinkedHashMap<>();
        for (Media media : mediaItems) {
            if (media.getId() == null) {
                throw new IllegalStateException("Cannot mirror popular media without a persisted media id");
            }
            distinctMedia.putIfAbsent(media.getId(), media);
        }
        return List.copyOf(distinctMedia.values());
    }

    private void markSyncSuccess(ContentSyncStatus status, BucketCounts bucketCounts, LocalDateTime syncedAt) {
        status.setLastAttemptedAt(syncedAt);
        status.setLastSuccessfulAt(syncedAt);
        status.setLastResult(ContentSyncResult.SUCCESS);
        status.setLastErrorMessage(null);
        status.setTrendingMoviesCount(bucketCounts.trendingMoviesCount());
        status.setTrendingShowsCount(bucketCounts.trendingShowsCount());
        status.setPopularNowCount(bucketCounts.popularNowCount());
        status.setAiringTodayCount(bucketCounts.airingTodayCount());
        status.setUpcomingCount(bucketCounts.upcomingCount());
        status.setRecommendedLaterCount(bucketCounts.recommendedLaterCount());
        contentSyncStatusRepository.save(status);
    }

    private void markSyncFailure(ContentSyncStatus status, LocalDateTime attemptedAt , RuntimeException ex) {
        status.setLastFailedAt(attemptedAt);
        status.setLastResult(ContentSyncResult.FAILURE);
        status.setLastErrorMessage(trimErrorMessage(ex.getMessage()));
        contentSyncStatusRepository.save(status);
    }

    private ContentSyncStatus getOrCreateStatus() {
        return contentSyncStatusRepository.findById(STATUS_KEY)
            .orElseGet(() -> ContentSyncStatus.builder()
                .statusKey(STATUS_KEY)
                .lastResult(ContentSyncResult.NEVER)
                .trendingMoviesCount(0)
                .trendingShowsCount(0)
                .popularNowCount(0)
                .airingTodayCount(0)
                .upcomingCount(0)
                .recommendedLaterCount(0)
                .build());
    }

    private String trimErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private record FetchedDiscoveryData(
        List<TmdbGenreDTO> movieGenres,
        List<TmdbGenreDTO> showGenres,
        List<TmdbMovieDTO> trendingMovies,
        List<TmdbMovieDTO> trendingShows,
        List<TmdbMovieDTO> popularMovies,
        List<TmdbMovieDTO> popularShows,
        List<TmdbMovieDTO> airingToday,
        List<TmdbMovieDTO> upcomingMovies,
        List<TmdbMovieDTO> onTheAir
    ) {}

    private record StoredDiscoveryData(
        Map<CuratedContentCategory, List<Media>> bucketMedia,
        BucketCounts bucketCounts,
        LocalDateTime syncedAt
    ) {}

    public record BucketCounts(
        Integer trendingMoviesCount,
        Integer trendingShowsCount,
        Integer popularNowCount,
        Integer airingTodayCount,
        Integer upcomingCount,
        Integer recommendedLaterCount
    ) {}

    private record AllMediaLists(
        List<Media> popularMovies,
        List<Media> popularShows,
        List<Media> upcomingMovies,
        List<Media> onTheAir,
        List<Media> trendingMovies,
        List<Media> trendingShows,
        List<Media> airingToday
    ) {}
}
