package com.project.watchmate.media.extras.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.project.watchmate.common.error.MediaNotFoundException;
import com.project.watchmate.common.error.TmdbClientException;
import com.project.watchmate.common.error.TmdbUnavailableException;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.extras.dto.MediaExtrasDTO;
import com.project.watchmate.media.extras.dto.TrailerDTO;
import com.project.watchmate.media.extras.dto.WatchProvidersDTO;
import com.project.watchmate.media.tmdb.client.TmdbClient;
import com.project.watchmate.media.tmdb.dto.TmdbCastMemberDTO;
import com.project.watchmate.media.tmdb.dto.TmdbCreditsDTO;
import com.project.watchmate.media.tmdb.dto.TmdbVideoDTO;
import com.project.watchmate.media.tmdb.dto.TmdbVideosResponseDTO;
import com.project.watchmate.media.tmdb.dto.TmdbWatchProviderEntryDTO;
import com.project.watchmate.media.tmdb.dto.TmdbWatchProviderRegionDTO;
import com.project.watchmate.media.tmdb.dto.TmdbWatchProvidersResponseDTO;

@ExtendWith(MockitoExtension.class)
class MediaExtrasServiceTest {

    private static final Long TMDB_ID = 550L;

    @Mock
    private TmdbClient tmdbClient;

    private MediaExtrasService mediaExtrasService;

    @BeforeEach
    void setUp() {
        mediaExtrasService = new MediaExtrasService(tmdbClient);
        ReflectionTestUtils.setField(mediaExtrasService, "defaultRegion", "US");
        ReflectionTestUtils.setField(mediaExtrasService, "castLimit", 10);
    }

    @Test
    void getExtras_sortsCastByOrderAscending() {
        stubVideosEmpty();
        stubProvidersEmpty();
        when(tmdbClient.fetchCredits(TMDB_ID, MediaType.MOVIE)).thenReturn(TmdbCreditsDTO.builder()
            .cast(List.of(cast(2, "Third"), cast(0, "First"), cast(1, "Second")))
            .build());

        MediaExtrasDTO result = mediaExtrasService.getExtras(TMDB_ID, MediaType.MOVIE);

        assertEquals(List.of("First", "Second", "Third"), result.cast().stream().map(member -> member.getName()).toList());
    }

    @Test
    void getExtras_limitsCastToConfiguredTen() {
        stubVideosEmpty();
        stubProvidersEmpty();
        List<TmdbCastMemberDTO> cast = java.util.stream.IntStream.range(0, 15)
            .mapToObj(i -> cast(i, "Member " + i))
            .toList();
        when(tmdbClient.fetchCredits(TMDB_ID, MediaType.MOVIE)).thenReturn(TmdbCreditsDTO.builder().cast(cast).build());

        MediaExtrasDTO result = mediaExtrasService.getExtras(TMDB_ID, MediaType.MOVIE);

        assertEquals(10, result.cast().size());
        assertEquals("Member 0", result.cast().get(0).getName());
        assertEquals("Member 9", result.cast().get(9).getName());
    }

    @Test
    void getExtras_emptyCastMapsToEmptyList() {
        stubVideosEmpty();
        stubProvidersEmpty();
        when(tmdbClient.fetchCredits(TMDB_ID, MediaType.MOVIE)).thenReturn(TmdbCreditsDTO.builder().cast(List.of()).build());

        MediaExtrasDTO result = mediaExtrasService.getExtras(TMDB_ID, MediaType.MOVIE);

        assertEquals(List.of(), result.cast());
    }

    @Test
    void getExtras_bestTrailerPrefersOfficialYoutubeTrailer() {
        stubCastEmpty();
        stubProvidersEmpty();
        when(tmdbClient.fetchVideos(TMDB_ID, MediaType.MOVIE)).thenReturn(TmdbVideosResponseDTO.builder()
            .results(List.of(
                video("unofficial-new", "YouTube", "Trailer", false, "2026-01-01T00:00:00.000Z"),
                video("official-trailer", "YouTube", "Trailer", true, "2020-01-01T00:00:00.000Z"),
                video("official-featurette", "YouTube", "Featurette", true, "2025-01-01T00:00:00.000Z")
            ))
            .build());

        TrailerDTO trailer = mediaExtrasService.getExtras(TMDB_ID, MediaType.MOVIE).bestTrailer();

        assertNotNull(trailer);
        assertEquals("official-trailer", trailer.getKey());
        assertEquals("https://www.youtube.com/watch?v=official-trailer", trailer.getYoutubeUrl());
        assertEquals("https://img.youtube.com/vi/official-trailer/hqdefault.jpg", trailer.getThumbnailUrl());
    }

    @Test
    void getExtras_bestTrailerFallsBackByPriorityAndNewestWithinTier() {
        stubCastEmpty();
        stubProvidersEmpty();
        when(tmdbClient.fetchVideos(TMDB_ID, MediaType.MOVIE)).thenReturn(TmdbVideosResponseDTO.builder()
            .results(List.of(
                video("old-official", "YouTube", "Clip", true, "2020-01-01T00:00:00.000Z"),
                video("new-official", "YouTube", "Behind the Scenes", true, "2024-01-01T00:00:00.000Z"),
                video("teaser", "YouTube", "Teaser", false, "2026-01-01T00:00:00.000Z")
            ))
            .build());

        TrailerDTO trailer = mediaExtrasService.getExtras(TMDB_ID, MediaType.MOVIE).bestTrailer();

        assertNotNull(trailer);
        assertEquals("new-official", trailer.getKey());
    }

    @Test
    void getExtras_bestTrailerFallsBackToTrailerThenTeaserThenAnyYoutube() {
        stubCastEmpty();
        stubProvidersEmpty();
        when(tmdbClient.fetchVideos(TMDB_ID, MediaType.MOVIE)).thenReturn(TmdbVideosResponseDTO.builder()
            .results(List.of(
                video("any-youtube", "YouTube", "Clip", false, "2026-01-01T00:00:00.000Z"),
                video("teaser", "YouTube", "Teaser", false, "2025-01-01T00:00:00.000Z"),
                video("trailer", "YouTube", "Trailer", false, "2024-01-01T00:00:00.000Z")
            ))
            .build());

        TrailerDTO trailer = mediaExtrasService.getExtras(TMDB_ID, MediaType.MOVIE).bestTrailer();

        assertNotNull(trailer);
        assertEquals("trailer", trailer.getKey());
    }

    @Test
    void getExtras_noYoutubeVideosReturnsNullTrailer() {
        stubCastEmpty();
        stubProvidersEmpty();
        when(tmdbClient.fetchVideos(TMDB_ID, MediaType.MOVIE)).thenReturn(TmdbVideosResponseDTO.builder()
            .results(List.of(video("vimeo", "Vimeo", "Trailer", true, "2026-01-01T00:00:00.000Z")))
            .build());

        assertNull(mediaExtrasService.getExtras(TMDB_ID, MediaType.MOVIE).bestTrailer());
    }

    @Test
    void getExtras_filtersWatchProvidersByUsRegion() {
        stubCastEmpty();
        stubVideosEmpty();
        when(tmdbClient.fetchWatchProviders(TMDB_ID, MediaType.MOVIE)).thenReturn(TmdbWatchProvidersResponseDTO.builder()
            .results(Map.of(
                "US", region("https://example.com/us", List.of(provider(2, "Apple TV", 1)), List.of(), List.of(), List.of(), List.of()),
                "GB", region("https://example.com/gb", List.of(provider(9, "Other", 0)), List.of(), List.of(), List.of(), List.of())
            ))
            .build());

        WatchProvidersDTO providers = mediaExtrasService.getExtras(TMDB_ID, MediaType.MOVIE).watchProviders();

        assertEquals("US", providers.getRegion());
        assertEquals("https://example.com/us", providers.getLink());
        assertEquals(List.of("Apple TV"), providers.getFlatrate().stream().map(entry -> entry.getProviderName()).toList());
    }

    @Test
    void getExtras_missingWatchProviderRegionReturnsEmptyLists() {
        stubCastEmpty();
        stubVideosEmpty();
        when(tmdbClient.fetchWatchProviders(TMDB_ID, MediaType.MOVIE)).thenReturn(TmdbWatchProvidersResponseDTO.builder()
            .results(Map.of("GB", region("https://example.com/gb", List.of(provider(9, "Other", 0)), List.of(), List.of(), List.of(), List.of())))
            .build());

        WatchProvidersDTO providers = mediaExtrasService.getExtras(TMDB_ID, MediaType.MOVIE).watchProviders();

        assertEquals("US", providers.getRegion());
        assertNull(providers.getLink());
        assertEquals(List.of(), providers.getFlatrate());
        assertEquals(List.of(), providers.getRent());
        assertEquals(List.of(), providers.getBuy());
        assertEquals(List.of(), providers.getAds());
        assertEquals(List.of(), providers.getFree());
    }

    @Test
    void getExtras_sortsProviderListsByDisplayPriority() {
        stubCastEmpty();
        stubVideosEmpty();
        when(tmdbClient.fetchWatchProviders(TMDB_ID, MediaType.MOVIE)).thenReturn(TmdbWatchProvidersResponseDTO.builder()
            .results(Map.of("US", region("https://example.com/us",
                List.of(provider(3, "Third", 3), provider(1, "First", 1), provider(2, "Second", 2)),
                List.of(provider(8, "Rent Second", 8), provider(4, "Rent First", 4)),
                List.of(), List.of(), List.of())))
            .build());

        WatchProvidersDTO providers = mediaExtrasService.getExtras(TMDB_ID, MediaType.MOVIE).watchProviders();

        assertEquals(List.of("First", "Second", "Third"), providers.getFlatrate().stream().map(entry -> entry.getProviderName()).toList());
        assertEquals(List.of("Rent First", "Rent Second"), providers.getRent().stream().map(entry -> entry.getProviderName()).toList());
    }

    @Test
    void getExtras_tmdbExceptionsDegradeGracefully() {
        when(tmdbClient.fetchCredits(TMDB_ID, MediaType.MOVIE)).thenThrow(new TmdbUnavailableException("down"));
        when(tmdbClient.fetchVideos(TMDB_ID, MediaType.MOVIE)).thenThrow(new MediaNotFoundException("missing"));
        when(tmdbClient.fetchWatchProviders(TMDB_ID, MediaType.MOVIE))
            .thenThrow(new TmdbClientException("bad", HttpStatus.BAD_GATEWAY, "TMDB_CLIENT_ERROR", null));

        MediaExtrasDTO result = mediaExtrasService.getExtras(TMDB_ID, MediaType.MOVIE);

        assertEquals(List.of(), result.cast());
        assertNull(result.bestTrailer());
        assertEquals("US", result.watchProviders().getRegion());
        assertEquals(List.of(), result.watchProviders().getFlatrate());
    }

    private void stubCastEmpty() {
        when(tmdbClient.fetchCredits(TMDB_ID, MediaType.MOVIE)).thenReturn(TmdbCreditsDTO.builder().cast(List.of()).build());
    }

    private void stubVideosEmpty() {
        when(tmdbClient.fetchVideos(TMDB_ID, MediaType.MOVIE)).thenReturn(TmdbVideosResponseDTO.builder().results(List.of()).build());
    }

    private void stubProvidersEmpty() {
        when(tmdbClient.fetchWatchProviders(TMDB_ID, MediaType.MOVIE)).thenReturn(TmdbWatchProvidersResponseDTO.builder().results(new HashMap<>()).build());
    }

    private TmdbCastMemberDTO cast(Integer order, String name) {
        return TmdbCastMemberDTO.builder()
            .id(100L + order)
            .name(name)
            .character("Character " + name)
            .profilePath(order == 0 ? null : "/" + name + ".jpg")
            .order(order)
            .knownForDepartment("Acting")
            .build();
    }

    private TmdbVideoDTO video(String key, String site, String type, Boolean official, String publishedAt) {
        return TmdbVideoDTO.builder()
            .key(key)
            .name("Video " + key)
            .site(site)
            .type(type)
            .official(official)
            .publishedAt(publishedAt)
            .build();
    }

    private TmdbWatchProviderRegionDTO region(
        String link,
        List<TmdbWatchProviderEntryDTO> flatrate,
        List<TmdbWatchProviderEntryDTO> rent,
        List<TmdbWatchProviderEntryDTO> buy,
        List<TmdbWatchProviderEntryDTO> ads,
        List<TmdbWatchProviderEntryDTO> free
    ) {
        return TmdbWatchProviderRegionDTO.builder()
            .link(link)
            .flatrate(flatrate)
            .rent(rent)
            .buy(buy)
            .ads(ads)
            .free(free)
            .build();
    }

    private TmdbWatchProviderEntryDTO provider(Integer id, String name, Integer priority) {
        return TmdbWatchProviderEntryDTO.builder()
            .providerId(id)
            .providerName(name)
            .logoPath("/" + id + ".jpg")
            .displayPriority(priority)
            .build();
    }
}
