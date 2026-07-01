package com.project.watchmate.show.metadata.application;

import com.project.watchmate.media.tmdb.application.TmdbService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.show.catalog.application.ShowCatalogService;
import com.project.watchmate.show.metadata.dto.PublicShowEpisodeMetadataDTO;
import com.project.watchmate.show.metadata.dto.PublicShowMetadataDTO;
import com.project.watchmate.show.metadata.dto.PublicShowSeasonMetadataDTO;
import com.project.watchmate.show.metadata.dto.ShowDetailsDTO;
import com.project.watchmate.show.metadata.dto.ShowEpisodeDetailsDTO;
import com.project.watchmate.common.error.UserNotFoundException;
import com.project.watchmate.common.mapper.WatchMateMapper;
import com.project.watchmate.show.metadata.mapper.ShowMetadataMapper;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.extras.application.MediaExtrasService;
import com.project.watchmate.media.extras.dto.CastMemberDTO;
import com.project.watchmate.media.extras.dto.MediaExtrasDTO;
import com.project.watchmate.media.extras.dto.TrailerDTO;
import com.project.watchmate.media.extras.dto.WatchProvidersDTO;
import com.project.watchmate.show.tracking.domain.UserShowTracking;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.review.persistence.ReviewRepository;
import com.project.watchmate.show.tracking.persistence.UserShowTrackingRepository;
import com.project.watchmate.user.persistence.UsersRepository;

@ExtendWith(MockitoExtension.class)
class ShowMetadataServiceTest {

    @Mock
    private ShowCatalogService showCatalogService;

    @Mock
    private TmdbService tmdbService;

    @Mock
    private ShowMetadataMapper showMetadataMapper;

    @Mock
    private WatchMateMapper watchMateMapper;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserShowTrackingRepository userShowTrackingRepository;

    @Mock
    private PublicShowMetadataCacheService publicShowMetadataCacheService;

    @Mock
    private MediaExtrasService mediaExtrasService;

    @InjectMocks
    private ShowMetadataService showMetadataService;

    private static final Long TMDB_ID = 9100L;
    private Users user;
    private Media show;

    @BeforeEach
    void setUp() {
        user = Users.builder().id(1L).username("user").favorites(new ArrayList<>()).build();
        show = Media.builder().id(10L).tmdbId(TMDB_ID).title("Test Show").type(MediaType.SHOW).reviews(new ArrayList<>()).build();
    }

    @Nested
    @DisplayName("Get Show Details Tests")
    class GetShowDetailsTests {

        @Test
        void getShowDetails_WhenPublicShowRequested_ReturnsMappedDto() {
            when(showCatalogService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
            when(showCatalogService.findImportedShow(TMDB_ID)).thenReturn(null);
            when(publicShowMetadataCacheService.getShowMetadata(TMDB_ID)).thenReturn(publicShowMetadata("Public Show"));
            when(mediaExtrasService.getExtras(TMDB_ID, MediaType.SHOW)).thenReturn(emptyExtras());

            ShowDetailsDTO result = showMetadataService.getShowDetails(TMDB_ID, MediaType.SHOW, null);

            assertNotNull(result);
            assertEquals(TMDB_ID, result.getTmdbId());
            assertEquals("Public Show", result.getTitle());
            assertEquals(WatchStatus.NONE, result.getWatchStatus());
            assertEquals(Boolean.FALSE, result.getIsFavourited());
            assertEquals(List.of(), result.getCast());
            assertEquals("US", result.getWatchProviders().getRegion());
        }

        @Test
        void getShowDetails_WhenAuthenticatedShowRequested_ReturnsMappedDto() {
            when(showCatalogService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
            when(showCatalogService.findImportedShow(TMDB_ID)).thenReturn(show);
            when(usersRepository.findByIdWithFavorites(1L)).thenReturn(Optional.of(user));
            when(reviewRepository.findByMedia(show)).thenReturn(List.of());
            when(userShowTrackingRepository.findByUserAndMedia(user, show)).thenReturn(Optional.empty());
            when(publicShowMetadataCacheService.getShowMetadata(TMDB_ID)).thenReturn(publicShowMetadata("Imported Show"));
            when(mediaExtrasService.getExtras(TMDB_ID, MediaType.SHOW)).thenReturn(extras());

            ShowDetailsDTO result = showMetadataService.getShowDetails(TMDB_ID, MediaType.SHOW, user);

            assertNotNull(result);
            assertEquals(TMDB_ID, result.getTmdbId());
            assertEquals("Imported Show", result.getTitle());
            assertEquals(WatchStatus.NONE, result.getWatchStatus());
            assertEquals("Show Actor", result.getCast().get(0).getName());
            assertEquals("show-trailer", result.getBestTrailer().getKey());
            assertEquals("US", result.getWatchProviders().getRegion());
        }

        @Test
        void getShowDetails_WhenUserNotFound_ThrowsUserNotFoundException() {
            when(showCatalogService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
            when(showCatalogService.findImportedShow(TMDB_ID)).thenReturn(show);
            when(usersRepository.findByIdWithFavorites(1L)).thenReturn(Optional.empty());

            UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> showMetadataService.getShowDetails(TMDB_ID, MediaType.SHOW, user));

            assertEquals("User not found", exception.getMessage());
        }

        @Test
        void getShowDetails_WhenTrackingExists_UsesCanonicalTrackingStatus() {
            when(showCatalogService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
            when(showCatalogService.findImportedShow(TMDB_ID)).thenReturn(show);
            when(usersRepository.findByIdWithFavorites(1L)).thenReturn(Optional.of(user));
            when(reviewRepository.findByMedia(show)).thenReturn(List.of());
            when(userShowTrackingRepository.findByUserAndMedia(user, show)).thenReturn(Optional.of(
                UserShowTracking.builder().user(user).media(show).status(WatchStatus.WATCHING).build()
            ));
            when(publicShowMetadataCacheService.getShowMetadata(TMDB_ID)).thenReturn(publicShowMetadata("Tracked Show"));
            when(mediaExtrasService.getExtras(TMDB_ID, MediaType.SHOW)).thenReturn(emptyExtras());

            ShowDetailsDTO result = showMetadataService.getShowDetails(TMDB_ID, MediaType.SHOW, user);

            assertEquals(WatchStatus.WATCHING, result.getWatchStatus());
        }
    }

    @Nested
    @DisplayName("Get Show Season Details Tests")
    class GetShowSeasonDetailsTests {

        @Test
        void getShowSeasonDetails_PreservesTmdbEpisodeIdIncludingLegacyNulls() {
            when(showCatalogService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
            when(publicShowMetadataCacheService.getSeasonMetadata(TMDB_ID, 1)).thenReturn(seasonWithEpisodes(2));

            Page<ShowEpisodeDetailsDTO> result = showMetadataService.getShowSeasonDetails(TMDB_ID, 1, MediaType.SHOW, null, 0, 20);

            assertNotNull(result);
            assertEquals(2, result.getTotalElements());
            assertEquals(4101L, result.getContent().get(0).getTmdbEpisodeId());
            assertNull(result.getContent().get(1).getTmdbEpisodeId());
        }

        @Test
        void getShowSeasonDetails_defaultPage_returnsAtMost20Episodes() {
            when(showCatalogService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
            when(publicShowMetadataCacheService.getSeasonMetadata(TMDB_ID, 1)).thenReturn(seasonWithEpisodes(25));

            Page<ShowEpisodeDetailsDTO> result = showMetadataService.getShowSeasonDetails(TMDB_ID, 1, MediaType.SHOW, null, 0, 20);

            assertEquals(25, result.getTotalElements());
            assertEquals(20, result.getContent().size());
            assertEquals(2, result.getTotalPages());
            assertTrue(result.isFirst());
            assertFalse(result.isLast());
        }

        @Test
        void getShowSeasonDetails_secondPage_returnsRemainingEpisodes() {
            when(showCatalogService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
            when(publicShowMetadataCacheService.getSeasonMetadata(TMDB_ID, 1)).thenReturn(seasonWithEpisodes(25));

            Page<ShowEpisodeDetailsDTO> result = showMetadataService.getShowSeasonDetails(TMDB_ID, 1, MediaType.SHOW, null, 1, 20);

            assertEquals(25, result.getTotalElements());
            assertEquals(5, result.getContent().size());
            assertFalse(result.isFirst());
            assertTrue(result.isLast());
        }

        @Test
        void getShowSeasonDetails_outOfRangePage_returnsEmptyPage() {
            when(showCatalogService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
            when(publicShowMetadataCacheService.getSeasonMetadata(TMDB_ID, 1)).thenReturn(seasonWithEpisodes(2));

            Page<ShowEpisodeDetailsDTO> result = showMetadataService.getShowSeasonDetails(TMDB_ID, 1, MediaType.SHOW, null, 5, 20);

            assertEquals(2, result.getTotalElements());
            assertEquals(0, result.getContent().size());
            assertTrue(result.isEmpty());
        }

        @Test
        void getShowSeasonDetails_episodesOrderedByEpisodeNumber() {
            when(showCatalogService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
            when(publicShowMetadataCacheService.getSeasonMetadata(TMDB_ID, 1)).thenReturn(seasonWithEpisodes(3));

            Page<ShowEpisodeDetailsDTO> result = showMetadataService.getShowSeasonDetails(TMDB_ID, 1, MediaType.SHOW, null, 0, 20);

            List<Integer> episodeNumbers = result.getContent().stream()
                .map(ShowEpisodeDetailsDTO::getEpisodeNumber)
                .toList();
            assertEquals(List.of(1, 2, 3), episodeNumbers);
        }

        @Test
        void getShowSeasonDetails_watchedFlagsCorrectForPagedEpisodes() {
            when(showCatalogService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
            when(publicShowMetadataCacheService.getSeasonMetadata(TMDB_ID, 1)).thenReturn(seasonWithEpisodes(3));
            when(showCatalogService.findImportedShow(TMDB_ID)).thenReturn(show);
            UserShowTracking tracking = UserShowTracking.builder()
                .user(user).media(show).status(com.project.watchmate.media.catalog.domain.WatchStatus.WATCHING)
                .episodeWatches(new ArrayList<>())
                .build();
            tracking.getEpisodeWatches().add(
                com.project.watchmate.show.tracking.domain.UserEpisodeWatch.builder()
                    .userShowTracking(tracking).seasonNumber(1).episodeNumber(2)
                    .watchedAt(java.time.LocalDateTime.now()).build()
            );
            when(userShowTrackingRepository.findWithEpisodeWatchesByUserAndMedia(user, show))
                .thenReturn(Optional.of(tracking));

            Page<ShowEpisodeDetailsDTO> result = showMetadataService.getShowSeasonDetails(TMDB_ID, 1, MediaType.SHOW, user, 0, 20);

            assertFalse(result.getContent().get(0).getWatched()); // ep 1
            assertTrue(result.getContent().get(1).getWatched());  // ep 2
            assertFalse(result.getContent().get(2).getWatched()); // ep 3
        }

        private PublicShowSeasonMetadataDTO seasonWithEpisodes(int count) {
            List<PublicShowEpisodeMetadataDTO> episodes = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                // episode 1 gets id 4101, episode 2 gets null (legacy test), rest get sequential ids
                Long episodeId;
                if (i == 1) episodeId = 4101L;
                else if (i == 2) episodeId = null;
                else episodeId = Long.valueOf(4100L + i);
                episodes.add(PublicShowEpisodeMetadataDTO.builder()
                    .tmdbEpisodeId(episodeId)
                    .seasonNumber(1)
                    .episodeNumber(i)
                    .name("Episode " + i)
                    .isAired(Boolean.TRUE)
                    .build());
            }
            return PublicShowSeasonMetadataDTO.builder()
                .tmdbId(TMDB_ID)
                .seasonNumber(1)
                .name("Season 1")
                .episodeCount(count)
                .episodes(episodes)
                .build();
        }
    }

    private PublicShowMetadataDTO publicShowMetadata(String title) {
        return PublicShowMetadataDTO.builder()
            .tmdbId(TMDB_ID)
            .title(title)
            .type(MediaType.SHOW)
            .genres(List.of())
            .seasons(List.of())
            .build();
    }

    private MediaExtrasDTO extras() {
        return new MediaExtrasDTO(
            List.of(CastMemberDTO.builder().tmdbPersonId(1L).name("Show Actor").order(0).build()),
            TrailerDTO.builder().key("show-trailer").youtubeUrl("https://www.youtube.com/watch?v=show-trailer").build(),
            WatchProvidersDTO.builder().region("US").flatrate(List.of()).rent(List.of()).buy(List.of()).ads(List.of()).free(List.of()).build()
        );
    }

    private MediaExtrasDTO emptyExtras() {
        return new MediaExtrasDTO(
            List.of(),
            null,
            WatchProvidersDTO.builder().region("US").flatrate(List.of()).rent(List.of()).buy(List.of()).ads(List.of()).free(List.of()).build()
        );
    }
}



