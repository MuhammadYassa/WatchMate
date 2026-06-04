package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.Dto.ShowDetailsDTO;
import com.project.watchmate.Dto.TmdbTvDetailsDTO;
import com.project.watchmate.Mappers.ShowMetadataMapper;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.UserShowTracking;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.ReviewRepository;
import com.project.watchmate.Repositories.UserShowTrackingRepository;
import com.project.watchmate.Repositories.UsersRepository;

@ExtendWith(MockitoExtension.class)
class ShowMetadataServiceTest {

    @Mock
    private ShowCatalogService showCatalogService;

    @Mock
    private TmdbService tmdbService;

    @Mock
    private ShowMetadataMapper showMetadataMapper;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserShowTrackingRepository userShowTrackingRepository;

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
            TmdbTvDetailsDTO tvDetails = TmdbTvDetailsDTO.builder().id(TMDB_ID).name("Public Show").build();
            ShowDetailsDTO expectedDto = ShowDetailsDTO.builder().tmdbId(TMDB_ID).title("Public Show").type(MediaType.SHOW).build();

            when(showCatalogService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
            when(showCatalogService.findImportedShow(TMDB_ID)).thenReturn(null);
            when(tmdbService.fetchTvDetails(TMDB_ID)).thenReturn(tvDetails);
            when(showMetadataMapper.mapToShowDetailsDTO(tvDetails, List.of(), Boolean.FALSE, WatchStatus.NONE)).thenReturn(expectedDto);

            ShowDetailsDTO result = showMetadataService.getShowDetails(TMDB_ID, MediaType.SHOW, null);

            assertNotNull(result);
            assertEquals(TMDB_ID, result.getTmdbId());
            verify(showCatalogService).validateShowType(MediaType.SHOW);
            verify(showMetadataMapper).mapToShowDetailsDTO(tvDetails, List.of(), Boolean.FALSE, WatchStatus.NONE);
        }

        @Test
        void getShowDetails_WhenAuthenticatedShowRequested_ReturnsMappedDto() {
            TmdbTvDetailsDTO tvDetails = TmdbTvDetailsDTO.builder().id(TMDB_ID).name("Imported Show").build();
            ShowDetailsDTO expectedDto = ShowDetailsDTO.builder()
                .tmdbId(TMDB_ID)
                .title("Imported Show")
                .type(MediaType.SHOW)
                .watchStatus(WatchStatus.NONE)
                .build();

            when(showCatalogService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
            when(showCatalogService.findImportedShow(TMDB_ID)).thenReturn(show);
            when(usersRepository.findByIdWithFavorites(1L)).thenReturn(Optional.of(user));
            when(reviewRepository.findByMedia(show)).thenReturn(List.of());
            when(userShowTrackingRepository.findByUserAndMedia(user, show)).thenReturn(Optional.empty());
            when(tmdbService.fetchTvDetails(TMDB_ID)).thenReturn(tvDetails);
            when(tmdbService.refreshShowSnapshot(show, tvDetails)).thenReturn(show);
            when(showMetadataMapper.mapToShowDetailsDTO(tvDetails, List.of(), Boolean.FALSE, WatchStatus.NONE)).thenReturn(expectedDto);

            ShowDetailsDTO result = showMetadataService.getShowDetails(TMDB_ID, MediaType.SHOW, user);

            assertNotNull(result);
            assertEquals(TMDB_ID, result.getTmdbId());
            verify(showMetadataMapper).mapToShowDetailsDTO(tvDetails, List.of(), Boolean.FALSE, WatchStatus.NONE);
        }

        @Test
        void getShowDetails_WhenUserNotFound_ThrowsRuntimeException() {
            when(showCatalogService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
            when(showCatalogService.findImportedShow(TMDB_ID)).thenReturn(show);
            when(usersRepository.findByIdWithFavorites(1L)).thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> showMetadataService.getShowDetails(TMDB_ID, MediaType.SHOW, user));

            assertEquals("User not found", exception.getMessage());
        }

        @Test
        void getShowDetails_WhenTrackingExists_UsesCanonicalTrackingStatus() {
            TmdbTvDetailsDTO tvDetails = TmdbTvDetailsDTO.builder().id(TMDB_ID).name("Tracked Show").build();
            ShowDetailsDTO expectedDto = ShowDetailsDTO.builder()
                .tmdbId(TMDB_ID)
                .title("Tracked Show")
                .type(MediaType.SHOW)
                .watchStatus(WatchStatus.WATCHING)
                .build();

            when(showCatalogService.validateShowType(MediaType.SHOW)).thenReturn(MediaType.SHOW);
            when(showCatalogService.findImportedShow(TMDB_ID)).thenReturn(show);
            when(usersRepository.findByIdWithFavorites(1L)).thenReturn(Optional.of(user));
            when(reviewRepository.findByMedia(show)).thenReturn(List.of());
            when(userShowTrackingRepository.findByUserAndMedia(user, show)).thenReturn(Optional.of(
                UserShowTracking.builder().user(user).media(show).status(WatchStatus.WATCHING).build()
            ));
            when(tmdbService.fetchTvDetails(TMDB_ID)).thenReturn(tvDetails);
            when(tmdbService.refreshShowSnapshot(show, tvDetails)).thenReturn(show);
            when(showMetadataMapper.mapToShowDetailsDTO(tvDetails, List.of(), Boolean.FALSE, WatchStatus.WATCHING)).thenReturn(expectedDto);

            ShowDetailsDTO result = showMetadataService.getShowDetails(TMDB_ID, MediaType.SHOW, user);

            assertEquals(WatchStatus.WATCHING, result.getWatchStatus());
            verify(userShowTrackingRepository).findByUserAndMedia(user, show);
        }
    }
}
