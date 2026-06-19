package com.project.watchmate.show.tracking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.project.watchmate.common.integration.support.AbstractIntegrationTest;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.show.jobs.application.ShowTrackingJobService;
import com.project.watchmate.show.jobs.domain.ShowTrackingJobStatus;
import com.project.watchmate.show.jobs.domain.ShowTrackingJobType;
import com.project.watchmate.show.jobs.dto.ShowTrackingJobDTO;
import com.project.watchmate.show.tracking.domain.UserShowTracking;
import com.project.watchmate.user.domain.Users;

class ShowTrackingFallbackPersistenceServiceIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private ShowTrackingJobService mockedShowTrackingJobService;

    @org.springframework.beans.factory.annotation.Autowired
    private ShowTrackingFallbackPersistenceService fallbackPersistenceService;

    @Test
    void saveProgressAndCreateJob_whenJobCreationFails_rollsBackNewTrackingRow() {
        Users user = saveUser("pointer-fallback-failure-user", true);
        Media show = saveMedia(9701L, "Pointer Fallback Failure", MediaType.SHOW);
        when(mockedShowTrackingJobService.createOrReuseSetShowProgressJob(user, show, 2, 1, 2))
            .thenThrow(new IllegalStateException("job creation failed"));

        assertThatThrownBy(() -> fallbackPersistenceService.saveProgressAndCreateJob(
            user,
            show,
            2,
            1,
            List.of(),
            List.of(),
            false,
            2
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("job creation failed");

        assertThat(userShowTrackingRepository.findByUserAndMedia(user, show)).isEmpty();
    }

    @Test
    void saveProgressAndCreateJob_whenJobCreationFails_rollsBackPointerMutation() {
        Users user = saveUser("pointer-fallback-existing-user", true);
        Media show = saveMedia(9702L, "Pointer Fallback Existing", MediaType.SHOW);
        userShowTrackingRepository.saveAndFlush(UserShowTracking.builder()
            .user(user)
            .media(show)
            .status(WatchStatus.WATCHING)
            .watchPositionSeason(1)
            .watchPositionEpisode(1)
            .episodeWatches(new java.util.ArrayList<>())
            .build());
        when(mockedShowTrackingJobService.createOrReuseSetShowProgressJob(user, show, 2, 3, 2))
            .thenThrow(new IllegalStateException("job creation failed"));

        assertThatThrownBy(() -> fallbackPersistenceService.saveProgressAndCreateJob(
            user,
            show,
            2,
            3,
            List.of(),
            List.of(),
            false,
            2
        ))
            .isInstanceOf(IllegalStateException.class);

        UserShowTracking persisted = userShowTrackingRepository.findByUserAndMedia(user, show).orElseThrow();
        assertThat(persisted.getWatchPositionSeason()).isEqualTo(1);
        assertThat(persisted.getWatchPositionEpisode()).isEqualTo(1);
    }

    @Test
    void saveProgressAndCreateJob_whenJobCreationSucceeds_persistsTrackingAndReturnsJob() {
        Users user = saveUser("pointer-fallback-success-user", true);
        Media show = saveMedia(9703L, "Pointer Fallback Success", MediaType.SHOW);
        when(mockedShowTrackingJobService.createOrReuseSetShowProgressJob(user, show, 2, 1, 2))
            .thenReturn(ShowTrackingJobDTO.builder()
                .jobId(88L)
                .status(ShowTrackingJobStatus.PENDING)
                .jobType(ShowTrackingJobType.SET_SHOW_PROGRESS)
                .tmdbId(show.getTmdbId())
                .mediaId(show.getId())
                .build());

        ShowTrackingJobDTO job = fallbackPersistenceService.saveProgressAndCreateJob(
            user,
            show,
            2,
            1,
            List.of(),
            List.of(),
            false,
            2
        );

        UserShowTracking persisted = userShowTrackingRepository.findByUserAndMedia(user, show).orElseThrow();
        assertThat(job.getJobId()).isEqualTo(88L);
        assertThat(persisted.getWatchPositionSeason()).isEqualTo(2);
        assertThat(persisted.getWatchPositionEpisode()).isEqualTo(1);
        assertThat(persisted.getStatus()).isEqualTo(WatchStatus.WATCHING);
    }

    @Test
    void ensureTrackingRowAndCreateStatusJob_whenJobCreationFails_rollsBackNewTrackingRow() {
        Users user = saveUser("status-fallback-failure-user", true);
        Media show = saveMedia(9704L, "Status Fallback Failure", MediaType.SHOW);
        when(mockedShowTrackingJobService.createOrReuseMarkUpToDateJob(user, show, 2))
            .thenThrow(new IllegalStateException("job creation failed"));

        assertThatThrownBy(() -> fallbackPersistenceService.ensureTrackingRowAndCreateStatusJob(
            user,
            show,
            WatchStatus.UP_TO_DATE,
            2
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("job creation failed");

        assertThat(userShowTrackingRepository.findByUserAndMedia(user, show)).isEmpty();
    }

    @Test
    void ensureTrackingRowAndCreateStatusJob_whenJobCreationSucceeds_persistsTrackingAndReturnsJob() {
        Users user = saveUser("status-fallback-success-user", true);
        Media show = saveMedia(9705L, "Status Fallback Success", MediaType.SHOW);
        when(mockedShowTrackingJobService.createOrReuseMarkUpToDateJob(user, show, 2))
            .thenReturn(ShowTrackingJobDTO.builder()
                .jobId(89L)
                .status(ShowTrackingJobStatus.PENDING)
                .jobType(ShowTrackingJobType.MARK_SHOW_UP_TO_DATE)
                .tmdbId(show.getTmdbId())
                .mediaId(show.getId())
                .build());

        ShowTrackingJobDTO job = fallbackPersistenceService.ensureTrackingRowAndCreateStatusJob(
            user,
            show,
            WatchStatus.UP_TO_DATE,
            2
        );

        UserShowTracking persisted = userShowTrackingRepository.findByUserAndMedia(user, show).orElseThrow();
        assertThat(job.getJobId()).isEqualTo(89L);
        assertThat(persisted.getStatus()).isEqualTo(WatchStatus.WATCHING);
    }
}
