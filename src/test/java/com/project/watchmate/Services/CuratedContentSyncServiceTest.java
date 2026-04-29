package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.project.watchmate.Clients.TmdbClient;
import com.project.watchmate.Dto.TmdbGenreDTO;
import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Models.ContentSyncStatus;
import com.project.watchmate.Models.CuratedContent;
import com.project.watchmate.Models.CuratedContentCategory;
import com.project.watchmate.Models.Genre;
import com.project.watchmate.Models.GenreLookup;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Repositories.ContentSyncStatusRepository;
import com.project.watchmate.Repositories.CuratedContentRepository;
import com.project.watchmate.Repositories.GenreLookupRepository;
import com.project.watchmate.Repositories.GenreRepository;
import com.project.watchmate.Repositories.PopularMediaRepository;

@ExtendWith(MockitoExtension.class)
class CuratedContentSyncServiceTest {

    @Mock
    private TmdbClient tmdbClient;

    @Mock
    private TmdbService tmdbService;

    @Mock
    private CuratedContentRepository curatedContentRepository;

    @Mock
    private GenreLookupRepository genreLookupRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private PopularMediaRepository popularMediaRepository;

    @Mock
    private ContentSyncStatusRepository contentSyncStatusRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Captor
    private ArgumentCaptor<List<GenreLookup>> lookupCaptor;

    @InjectMocks
    private CuratedContentSyncService curatedContentSyncService;

    @BeforeEach
    void setUp() {
        TransactionStatus transactionStatus = new SimpleTransactionStatus();
        lenient().when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        lenient().when(genreRepository.findById(anyLong())).thenReturn(Optional.empty());
    }

    @Test
    void syncDiscoveryContent_whenRemoteFetchFails_doesNotReplaceBuckets() {
        when(contentSyncStatusRepository.findById(CuratedContentSyncService.STATUS_KEY)).thenReturn(Optional.empty());
        when(tmdbClient.fetchGenres("movie")).thenThrow(new IllegalStateException("TMDB unavailable"));

        curatedContentSyncService.syncDiscoveryContent("test");

        verify(curatedContentRepository, times(0)).deleteByCategoryKey(any());
        verify(popularMediaRepository, times(0)).deleteAll();
    }

    @Test
    void syncDiscoveryContent_whenSyncAlreadyInProgress_doesNotCallRepositories() {
        setSyncInProgress(true);

        curatedContentSyncService.syncDiscoveryContent("test");

        verifyNoInteractions(contentSyncStatusRepository, curatedContentRepository, genreLookupRepository, genreRepository, popularMediaRepository);
    }

    @Test
    void syncDiscoveryContent_whenSuccessful_callsDeleteByCategoryKeyOncePerBucketCategory() {
        stubSuccessfulSync(
            List.of(media(1L, 101L, MediaType.MOVIE, 8.5, LocalDate.of(2024, 1, 1))),
            List.of(media(2L, 201L, MediaType.SHOW, 8.1, LocalDate.of(2024, 2, 1))),
            List.of(media(3L, 301L, MediaType.MOVIE, 7.8, LocalDate.of(2023, 3, 1))),
            List.of(media(4L, 401L, MediaType.SHOW, 7.7, LocalDate.of(2023, 4, 1))),
            List.of(media(5L, 501L, MediaType.SHOW, 7.5, LocalDate.of(2025, 1, 1))),
            List.of(media(6L, 601L, MediaType.MOVIE, 7.9, LocalDate.of(2026, 1, 1))),
            List.of(media(7L, 701L, MediaType.SHOW, 8.0, LocalDate.of(2026, 2, 1)))
        );

        curatedContentSyncService.syncDiscoveryContent("test");

        verify(curatedContentRepository, times(1)).deleteByCategoryKey(CuratedContentCategory.TRENDING_MOVIES);
        verify(curatedContentRepository, times(1)).deleteByCategoryKey(CuratedContentCategory.TRENDING_SHOWS);
        verify(curatedContentRepository, times(1)).deleteByCategoryKey(CuratedContentCategory.POPULAR_NOW);
        verify(curatedContentRepository, times(1)).deleteByCategoryKey(CuratedContentCategory.AIRING_TODAY);
        verify(curatedContentRepository, times(1)).deleteByCategoryKey(CuratedContentCategory.UPCOMING);
        verify(curatedContentRepository, times(1)).deleteByCategoryKey(CuratedContentCategory.RECOMMENDED_LATER);
    }

    @Test
    void syncDiscoveryContent_whenSuccessful_callsPopularMediaDeleteAllExactlyOnce() {
        stubSuccessfulSync(
            List.of(media(1L, 101L, MediaType.MOVIE, 8.5, LocalDate.of(2024, 1, 1))),
            List.of(media(2L, 201L, MediaType.SHOW, 8.1, LocalDate.of(2024, 2, 1))),
            List.of(media(3L, 301L, MediaType.MOVIE, 7.8, LocalDate.of(2023, 3, 1))),
            List.of(media(4L, 401L, MediaType.SHOW, 7.7, LocalDate.of(2023, 4, 1))),
            List.of(media(5L, 501L, MediaType.SHOW, 7.5, LocalDate.of(2025, 1, 1))),
            List.of(media(6L, 601L, MediaType.MOVIE, 7.9, LocalDate.of(2026, 1, 1))),
            List.of(media(7L, 701L, MediaType.SHOW, 8.0, LocalDate.of(2026, 2, 1)))
        );

        curatedContentSyncService.syncDiscoveryContent("test");

        verify(popularMediaRepository, times(1)).deleteAllInBatch();
    }

    @Test
    void syncDiscoveryContent_whenSuccessful_buildsPopularNowInAlternatingOrder() {
        Map<CuratedContentCategory, List<CuratedContent>> savedBatches = captureCuratedContentBatches();
        Media movieOne = media(11L, 311L, MediaType.MOVIE, 8.1, LocalDate.of(2024, 1, 1));
        Media movieTwo = media(12L, 312L, MediaType.MOVIE, 8.0, LocalDate.of(2024, 1, 2));
        Media movieThree = media(13L, 313L, MediaType.MOVIE, 7.9, LocalDate.of(2024, 1, 3));
        Media showOne = media(21L, 411L, MediaType.SHOW, 8.2, LocalDate.of(2024, 2, 1));
        Media showTwo = media(22L, 412L, MediaType.SHOW, 8.1, LocalDate.of(2024, 2, 2));
        stubSuccessfulSync(
            List.of(),
            List.of(),
            List.of(movieOne, movieTwo, movieThree),
            List.of(showOne, showTwo),
            List.of(),
            List.of(),
            List.of()
        );

        curatedContentSyncService.syncDiscoveryContent("test");

        assertIterableEquals(
            List.of(11L, 21L, 12L, 22L, 13L),
            mediaIds(savedBatches.get(CuratedContentCategory.POPULAR_NOW))
        );
    }

    @Test
    void syncDiscoveryContent_whenSuccessful_excludesAlreadyBucketedMediaFromRecommendedLater() {
        Map<CuratedContentCategory, List<CuratedContent>> savedBatches = captureCuratedContentBatches();
        Media alreadyBucketed = media(50L, 150L, MediaType.MOVIE, 8.7, LocalDate.of(2024, 1, 1));
        Media allowedLeftover = media(51L, 151L, MediaType.MOVIE, 8.8, LocalDate.of(2024, 1, 2));
        List<Media> popularMovies = new ArrayList<>(mediaRange(100L, 1000L, 20, MediaType.MOVIE, 7.5, LocalDate.of(2024, 3, 1)));
        popularMovies.add(alreadyBucketed);
        popularMovies.add(allowedLeftover);
        stubSuccessfulSync(
            List.of(alreadyBucketed),
            List.of(),
            popularMovies,
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );

        curatedContentSyncService.syncDiscoveryContent("test");

        assertIterableEquals(
            List.of(51L),
            mediaIds(savedBatches.get(CuratedContentCategory.RECOMMENDED_LATER))
        );
    }

    @Test
    void syncDiscoveryContent_whenSuccessful_sortsRecommendedLaterByRatingReleaseDateAndIdDescending() {
        Map<CuratedContentCategory, List<CuratedContent>> savedBatches = captureCuratedContentBatches();
        Media candidateByRating = media(902L, 1902L, MediaType.MOVIE, 9.0, LocalDate.of(2023, 1, 1));
        Media candidateByIdHigh = media(904L, 1904L, MediaType.MOVIE, 8.5, LocalDate.of(2025, 1, 1));
        Media candidateByIdLow = media(903L, 1903L, MediaType.MOVIE, 8.5, LocalDate.of(2025, 1, 1));
        Media candidateByDate = media(901L, 1901L, MediaType.MOVIE, 8.5, LocalDate.of(2024, 1, 1));
        List<Media> popularMovies = new ArrayList<>(mediaRange(200L, 2200L, 20, MediaType.MOVIE, 7.4, LocalDate.of(2024, 4, 1)));
        popularMovies.add(candidateByDate);
        popularMovies.add(candidateByRating);
        popularMovies.add(candidateByIdLow);
        popularMovies.add(candidateByIdHigh);
        stubSuccessfulSync(
            List.of(),
            List.of(),
            popularMovies,
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );

        curatedContentSyncService.syncDiscoveryContent("test");

        assertIterableEquals(
            List.of(902L, 904L, 903L, 901L),
            mediaIds(savedBatches.get(CuratedContentCategory.RECOMMENDED_LATER))
        );
    }

    @Test
    void syncDiscoveryContent_whenSuccessful_savesFinalStatusWithCorrectCountsForEveryBucket() {
        List<ContentSyncStatus> savedStatuses = captureSavedStatuses();
        List<Media> popularMovies = new ArrayList<>(mediaRange(300L, 3300L, 21, MediaType.MOVIE, 7.9, LocalDate.of(2024, 5, 1)));
        List<Media> popularShows = mediaRange(400L, 4400L, 2, MediaType.SHOW, 7.8, LocalDate.of(2024, 6, 1));
        List<Media> upcomingMovies = new ArrayList<>(mediaRange(500L, 5500L, 21, MediaType.MOVIE, 8.0, LocalDate.of(2025, 1, 1)));
        List<Media> onTheAir = List.of(media(600L, 6600L, MediaType.SHOW, 8.1, LocalDate.of(2025, 2, 1)));
        stubSuccessfulSync(
            List.of(
                media(1L, 101L, MediaType.MOVIE, 8.5, LocalDate.of(2024, 1, 1)),
                media(2L, 102L, MediaType.MOVIE, 8.4, LocalDate.of(2024, 1, 2))
            ),
            List.of(media(3L, 201L, MediaType.SHOW, 8.3, LocalDate.of(2024, 2, 1))),
            popularMovies,
            popularShows,
            List.of(media(4L, 301L, MediaType.SHOW, 7.9, LocalDate.of(2025, 3, 1))),
            upcomingMovies,
            onTheAir
        );

        curatedContentSyncService.syncDiscoveryContent("test");

        ContentSyncStatus finalStatus = savedStatuses.get(savedStatuses.size() - 1);
        assertAll(
            () -> assertEquals(Integer.valueOf(2), finalStatus.getTrendingMoviesCount()),
            () -> assertEquals(Integer.valueOf(1), finalStatus.getTrendingShowsCount()),
            () -> assertEquals(Integer.valueOf(20), finalStatus.getPopularNowCount()),
            () -> assertEquals(Integer.valueOf(1), finalStatus.getAiringTodayCount()),
            () -> assertEquals(Integer.valueOf(20), finalStatus.getUpcomingCount()),
            () -> assertEquals(Integer.valueOf(5), finalStatus.getRecommendedLaterCount())
        );
    }

    @Test
    void syncDiscoveryContent_whenSuccessful_callsContentSyncStatusSaveExactlyTwice() {
        when(contentSyncStatusRepository.findById(CuratedContentSyncService.STATUS_KEY)).thenReturn(Optional.empty());
        stubRemoteAndMediaSync(
            List.of(media(1L, 101L, MediaType.MOVIE, 8.5, LocalDate.of(2024, 1, 1))),
            List.of(media(2L, 201L, MediaType.SHOW, 8.1, LocalDate.of(2024, 2, 1))),
            List.of(media(3L, 301L, MediaType.MOVIE, 7.8, LocalDate.of(2023, 3, 1))),
            List.of(media(4L, 401L, MediaType.SHOW, 7.7, LocalDate.of(2023, 4, 1))),
            List.of(media(5L, 501L, MediaType.SHOW, 7.5, LocalDate.of(2025, 1, 1))),
            List.of(media(6L, 601L, MediaType.MOVIE, 7.9, LocalDate.of(2026, 1, 1))),
            List.of(media(7L, 701L, MediaType.SHOW, 8.0, LocalDate.of(2026, 2, 1)))
        );

        curatedContentSyncService.syncDiscoveryContent("test");

        verify(contentSyncStatusRepository, times(2)).save(any(ContentSyncStatus.class));
    }

    @Test
    void syncDiscoveryContent_whenSyncFails_setsLastFailedAtToLastAttemptedAt() {
        List<ContentSyncStatus> savedStatuses = captureSavedStatuses();
        when(contentSyncStatusRepository.findById(CuratedContentSyncService.STATUS_KEY)).thenReturn(Optional.empty());
        when(tmdbClient.fetchGenres("movie")).thenThrow(new IllegalStateException("TMDB unavailable"));

        curatedContentSyncService.syncDiscoveryContent("test");

        ContentSyncStatus finalStatus = savedStatuses.get(savedStatuses.size() - 1);
        assertEquals(finalStatus.getLastAttemptedAt(), finalStatus.getLastFailedAt());
    }

    @Test
    void syncGenres_whenMovieAndShowGenresProvided_upsertsGenresForBothTypes() {
        ArgumentCaptor<Genre> genreCaptor = ArgumentCaptor.forClass(Genre.class);
        LocalDateTime syncedAt = LocalDateTime.of(2026, 4, 29, 12, 0);
        List<TmdbGenreDTO> movieGenres = List.of(new TmdbGenreDTO(28L, "Action"));
        List<TmdbGenreDTO> showGenres = List.of(new TmdbGenreDTO(18L, "Drama"));

        curatedContentSyncService.syncGenres(movieGenres, showGenres, syncedAt);

        verify(genreRepository, times(2)).save(genreCaptor.capture());
        assertIterableEquals(
            List.of("28:Action", "18:Drama"),
            genreCaptor.getAllValues().stream()
                .map(genre -> genre.getId() + ":" + genre.getName())
                .toList()
        );
    }

    @Test
    void syncGenres_whenMovieAndShowGenresProvided_replacesGenreLookupForEachType() {
        LocalDateTime syncedAt = LocalDateTime.of(2026, 4, 29, 12, 0);
        List<TmdbGenreDTO> movieGenres = List.of(new TmdbGenreDTO(28L, "Action"));
        List<TmdbGenreDTO> showGenres = List.of(new TmdbGenreDTO(18L, "Drama"));

        curatedContentSyncService.syncGenres(movieGenres, showGenres, syncedAt);

        verify(genreLookupRepository, times(1)).deleteByMediaType(MediaType.MOVIE);
        verify(genreLookupRepository, times(1)).deleteByMediaType(MediaType.SHOW);
        verify(genreLookupRepository, times(2)).saveAll(lookupCaptor.capture());
        assertAll(
            () -> assertLookupBatch(lookupCaptor.getAllValues().get(0), MediaType.MOVIE, syncedAt, List.of("28:Action")),
            () -> assertLookupBatch(lookupCaptor.getAllValues().get(1), MediaType.SHOW, syncedAt, List.of("18:Drama"))
        );
    }

    @Test
    void syncDiscoveryContent_whenExistingStatusPresent_updatesThatExistingEntity() {
        ArgumentCaptor<ContentSyncStatus> statusCaptor = ArgumentCaptor.forClass(ContentSyncStatus.class);
        ContentSyncStatus existingStatus = ContentSyncStatus.builder()
            .statusKey(CuratedContentSyncService.STATUS_KEY)
            .build();
        when(contentSyncStatusRepository.findById(CuratedContentSyncService.STATUS_KEY)).thenReturn(Optional.of(existingStatus));
        when(tmdbClient.fetchGenres("movie")).thenThrow(new IllegalStateException("TMDB unavailable"));

        curatedContentSyncService.syncDiscoveryContent("test");

        verify(contentSyncStatusRepository, times(2)).save(statusCaptor.capture());
        assertAll(
            () -> assertSame(existingStatus, statusCaptor.getAllValues().get(0)),
            () -> assertSame(existingStatus, statusCaptor.getAllValues().get(1))
        );
    }

    private void stubSuccessfulSync(
        List<Media> trendingMovies,
        List<Media> trendingShows,
        List<Media> popularMovies,
        List<Media> popularShows,
        List<Media> airingToday,
        List<Media> upcomingMovies,
        List<Media> onTheAir
    ) {
        when(contentSyncStatusRepository.findById(CuratedContentSyncService.STATUS_KEY)).thenReturn(Optional.empty());
        stubRemoteAndMediaSync(trendingMovies, trendingShows, popularMovies, popularShows, airingToday, upcomingMovies, onTheAir);
    }

    private void stubRemoteAndMediaSync(
        List<Media> trendingMovies,
        List<Media> trendingShows,
        List<Media> popularMovies,
        List<Media> popularShows,
        List<Media> airingToday,
        List<Media> upcomingMovies,
        List<Media> onTheAir
    ) {
        List<TmdbGenreDTO> movieGenres = List.of(new TmdbGenreDTO(28L, "Action"));
        List<TmdbGenreDTO> showGenres = List.of(new TmdbGenreDTO(18L, "Drama"));
        List<TmdbMovieDTO> trendingMovieDtos = toTmdbMovies(trendingMovies);
        List<TmdbMovieDTO> trendingShowDtos = toTmdbMovies(trendingShows);
        List<TmdbMovieDTO> popularMovieDtos = toTmdbMovies(popularMovies);
        List<TmdbMovieDTO> popularShowDtos = toTmdbMovies(popularShows);
        List<TmdbMovieDTO> airingTodayDtos = toTmdbMovies(airingToday);
        List<TmdbMovieDTO> upcomingMovieDtos = toTmdbMovies(upcomingMovies);
        List<TmdbMovieDTO> onTheAirDtos = toTmdbMovies(onTheAir);

        when(tmdbClient.fetchGenres("movie")).thenReturn(movieGenres);
        when(tmdbClient.fetchGenres("tv")).thenReturn(showGenres);
        when(tmdbClient.fetchTrending("movie")).thenReturn(trendingMovieDtos);
        when(tmdbClient.fetchTrending("tv")).thenReturn(trendingShowDtos);
        when(tmdbClient.fetchPopular("movie")).thenReturn(popularMovieDtos);
        when(tmdbClient.fetchPopular("tv")).thenReturn(popularShowDtos);
        when(tmdbClient.fetchAiringToday()).thenReturn(airingTodayDtos);
        when(tmdbClient.fetchUpcomingMovies()).thenReturn(upcomingMovieDtos);
        when(tmdbClient.fetchOnTheAir()).thenReturn(onTheAirDtos);

        when(tmdbService.upsertMediaFromTmdb(trendingMovieDtos, MediaType.MOVIE)).thenReturn(trendingMovies);
        when(tmdbService.upsertMediaFromTmdb(trendingShowDtos, MediaType.SHOW)).thenReturn(trendingShows);
        when(tmdbService.upsertMediaFromTmdb(popularMovieDtos, MediaType.MOVIE)).thenReturn(popularMovies);
        when(tmdbService.upsertMediaFromTmdb(popularShowDtos, MediaType.SHOW)).thenReturn(popularShows);
        when(tmdbService.upsertMediaFromTmdb(airingTodayDtos, MediaType.SHOW)).thenReturn(airingToday);
        when(tmdbService.upsertMediaFromTmdb(upcomingMovieDtos, MediaType.MOVIE)).thenReturn(upcomingMovies);
        when(tmdbService.upsertMediaFromTmdb(onTheAirDtos, MediaType.SHOW)).thenReturn(onTheAir);
    }

    private Map<CuratedContentCategory, List<CuratedContent>> captureCuratedContentBatches() {
        Map<CuratedContentCategory, List<CuratedContent>> savedBatches = new EnumMap<>(CuratedContentCategory.class);
        when(curatedContentRepository.saveAll(anyList())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<CuratedContent> batch = new ArrayList<>((List<CuratedContent>) invocation.getArgument(0));
            if (!batch.isEmpty()) {
                savedBatches.put(batch.get(0).getCategoryKey(), batch);
            }
            return invocation.getArgument(0);
        });
        return savedBatches;
    }

    private List<ContentSyncStatus> captureSavedStatuses() {
        List<ContentSyncStatus> savedStatuses = new ArrayList<>();
        when(contentSyncStatusRepository.save(any(ContentSyncStatus.class))).thenAnswer(invocation -> {
            savedStatuses.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        return savedStatuses;
    }

    private void assertLookupBatch(
        List<GenreLookup> lookups,
        MediaType mediaType,
        LocalDateTime syncedAt,
        List<String> expectedIdAndName
    ) {
        assertAll(
            () -> assertEquals(expectedIdAndName, lookups.stream().map(lookup -> lookup.getTmdbGenreId() + ":" + lookup.getName()).toList()),
            () -> assertEquals(List.of(mediaType), lookups.stream().map(GenreLookup::getMediaType).distinct().toList()),
            () -> assertEquals(List.of(syncedAt), lookups.stream().map(GenreLookup::getSyncedAt).distinct().toList())
        );
    }

    private void setSyncInProgress(boolean inProgress) {
        try {
            Field field = CuratedContentSyncService.class.getDeclaredField("syncInProgress");
            field.setAccessible(true);
            AtomicBoolean syncFlag = (AtomicBoolean) field.get(curatedContentSyncService);
            syncFlag.set(inProgress);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private List<TmdbMovieDTO> toTmdbMovies(List<Media> mediaItems) {
        return mediaItems.stream()
            .map(media -> TmdbMovieDTO.builder()
                .id(media.getTmdbId())
                .title(media.getTitle())
                .voteAverage(media.getRating())
                .releaseDate(media.getReleaseDate() == null ? null : media.getReleaseDate().toString())
                .build())
            .toList();
    }

    private List<Long> mediaIds(List<CuratedContent> curatedContent) {
        return curatedContent.stream()
            .map(content -> content.getMedia().getId())
            .toList();
    }

    private List<Media> mediaRange(
        long startId,
        long startTmdbId,
        int count,
        MediaType mediaType,
        double rating,
        LocalDate startDate
    ) {
        List<Media> mediaItems = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            mediaItems.add(media(
                startId + index,
                startTmdbId + index,
                mediaType,
                rating,
                startDate.plusDays(index)
            ));
        }
        return mediaItems;
    }

    private Media media(Long id, Long tmdbId, MediaType mediaType, Double rating, LocalDate releaseDate) {
        return Media.builder()
            .id(id)
            .tmdbId(tmdbId)
            .title("Media " + tmdbId)
            .type(mediaType)
            .rating(rating)
            .releaseDate(releaseDate)
            .genres(List.of())
            .build();
    }
}
