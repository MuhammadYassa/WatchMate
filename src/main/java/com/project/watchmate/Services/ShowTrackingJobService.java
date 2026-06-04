package com.project.watchmate.Services;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.project.watchmate.Dto.ShowTrackingJobDTO;
import com.project.watchmate.Dto.TmdbTvDetailsDTO;
import com.project.watchmate.Exception.ShowTrackingJobNotFoundException;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.ShowEpisode;
import com.project.watchmate.Models.ShowTrackingJob;
import com.project.watchmate.Models.ShowTrackingJobStatus;
import com.project.watchmate.Models.ShowTrackingJobType;
import com.project.watchmate.Models.UserShowTracking;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.ShowTrackingJobRepository;
import com.project.watchmate.Repositories.UserShowTrackingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShowTrackingJobService {

    private final ShowTrackingJobRepository showTrackingJobRepository;

    private final UserShowTrackingRepository userShowTrackingRepository;

    private final ShowCatalogService showCatalogService;

    private final ShowTrackingWriteSupport showTrackingWriteSupport;

    private final ShowTrackingJobProperties showTrackingJobProperties;

    private final PlatformTransactionManager transactionManager;

    public ShowTrackingJobDTO createOrReuseHydrateCatalogJob(Users user, Media media, Integer totalSeasons) {
        return createOrReuseJob(
            user,
            media,
            ShowTrackingJobType.HYDRATE_SHOW_CATALOG,
            null,
            null,
            null,
            totalSeasons
        );
    }

    public ShowTrackingJobDTO createOrReuseMarkWatchedJob(Users user, Media media, Integer totalSeasons) {
        return createOrReuseJob(
            user,
            media,
            ShowTrackingJobType.MARK_SHOW_WATCHED,
            WatchStatus.WATCHED,
            null,
            null,
            totalSeasons
        );
    }

    public ShowTrackingJobDTO createOrReuseMarkUpToDateJob(Users user, Media media, Integer totalSeasons) {
        return createOrReuseJob(
            user,
            media,
            ShowTrackingJobType.MARK_SHOW_UP_TO_DATE,
            WatchStatus.UP_TO_DATE,
            null,
            null,
            totalSeasons
        );
    }

    public ShowTrackingJobDTO createOrReuseMarkPreviousEpisodesWatchedJob(
        Users user,
        Media media,
        Integer targetSeasonNumber,
        Integer targetEpisodeNumber,
        Integer totalSeasons
    ) {
        return createOrReuseJob(
            user,
            media,
            ShowTrackingJobType.MARK_PREVIOUS_EPISODES_WATCHED,
            null,
            targetSeasonNumber,
            targetEpisodeNumber,
            totalSeasons
        );
    }

    @Transactional(readOnly = true)
    public ShowTrackingJobDTO getUserJob(Users user, Long jobId) {
        ShowTrackingJob job = showTrackingJobRepository.findVisibleUserJob(jobId, user)
            .orElseThrow(() -> new ShowTrackingJobNotFoundException("Show tracking job not found."));
        return toDto(job);
    }

    @Scheduled(fixedDelayString = "${watchmate.show-jobs.poll-delay-ms:5000}")
    public void pollPendingJobs() {
        if (!showTrackingJobProperties.isEnabled()) {
            return;
        }

        recoverStaleRunningJobs();
        List<ShowTrackingJob> candidates = showTrackingJobRepository.findByStatusOrderByCreatedAtAsc(
            ShowTrackingJobStatus.PENDING,
            PageRequest.of(0, showTrackingJobProperties.getMaxJobsPerPoll())
        );

        for (ShowTrackingJob candidate : candidates) {
            if (claimPendingJob(candidate.getId())) {
                processClaimedJob(candidate.getId());
            }
        }
    }

    public boolean claimPendingJob(Long jobId) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            LocalDateTime now = LocalDateTime.now();
            return showTrackingJobRepository.claimPendingJob(
                jobId,
                ShowTrackingJobStatus.PENDING,
                ShowTrackingJobStatus.RUNNING,
                now,
                now
            ) == 1;
        });
    }

    private ShowTrackingJobDTO createOrReuseJob(
        Users user,
        Media media,
        ShowTrackingJobType jobType,
        WatchStatus requestedStatus,
        Integer targetSeasonNumber,
        Integer targetEpisodeNumber,
        Integer totalSeasons
    ) {
        List<ShowTrackingJob> activeJobs = showTrackingJobRepository.findMatchingActiveJobs(
            user,
            media,
            jobType,
            requestedStatus,
            targetSeasonNumber,
            targetEpisodeNumber,
            List.of(ShowTrackingJobStatus.PENDING, ShowTrackingJobStatus.RUNNING),
            PageRequest.of(0, 1)
        );
        if (!activeJobs.isEmpty()) {
            return toDto(activeJobs.get(0));
        }

        ShowTrackingJob job = showTrackingJobRepository.save(ShowTrackingJob.builder()
            .user(user)
            .media(media)
            .jobType(jobType)
            .status(ShowTrackingJobStatus.PENDING)
            .requestedStatus(requestedStatus)
            .targetSeasonNumber(targetSeasonNumber)
            .targetEpisodeNumber(targetEpisodeNumber)
            .totalSeasons(totalSeasons)
            .completedSeasons(0)
            .build());
        return toDto(job);
    }

    private void recoverStaleRunningJobs() {
        if (!showTrackingJobProperties.isEnabled()) {
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(showTrackingJobProperties.getStaleRunningMinutes());
        List<ShowTrackingJob> staleJobs = showTrackingJobRepository.findByStatusAndStartedAtBeforeOrderByStartedAtAsc(
            ShowTrackingJobStatus.RUNNING,
            cutoff
        );
        for (ShowTrackingJob job : staleJobs) {
            if (job.getAttemptCount() != null && job.getAttemptCount() >= showTrackingJobProperties.getMaxAttempts()) {
                failJob(job.getId(), "JOB_RETRY_EXHAUSTED", "Show tracking job exceeded the maximum retry limit.");
                continue;
            }
            requeueJob(job.getId());
        }
    }

    private void processClaimedJob(Long jobId) {
        ShowTrackingJob job = showTrackingJobRepository.findById(jobId)
            .orElseThrow(() -> new ShowTrackingJobNotFoundException("Show tracking job not found."));

        try {
            switch (job.getJobType()) {
                case HYDRATE_SHOW_CATALOG -> processHydrateCatalogJob(job);
                case MARK_SHOW_WATCHED -> processMarkWatchedJob(job);
                case MARK_SHOW_UP_TO_DATE -> processMarkUpToDateJob(job);
                case MARK_PREVIOUS_EPISODES_WATCHED -> processMarkPreviousEpisodesWatchedJob(job);
            }
            completeJob(jobId);
        } catch (RuntimeException ex) {
            log.warn("Show tracking job failed id={} type={}", jobId, job.getJobType(), ex);
            failJob(jobId, resolveErrorCode(ex), trimErrorMessage(ex.getMessage()));
        }
    }

    private void processHydrateCatalogJob(ShowTrackingJob job) {
        Media media = job.getMedia();
        TmdbTvDetailsDTO tvDetails = showCatalogService.fetchAndRefreshShowDetails(media.getTmdbId(), media);
        List<Integer> requiredSeasons = showCatalogService.getRequiredStandardSeasonNumbers(tvDetails);
        hydrateRequiredSeasons(job, media, requiredSeasons);
    }

    private void processMarkWatchedJob(ShowTrackingJob job) {
        Media media = job.getMedia();
        TmdbTvDetailsDTO tvDetails = showCatalogService.fetchAndRefreshShowDetails(media.getTmdbId(), media);
        boolean endedShow = showCatalogService.isEndedShow(tvDetails);
        List<Integer> requiredSeasons = endedShow
            ? showCatalogService.getRequiredStandardSeasonNumbers(tvDetails)
            : showCatalogService.getRequiredAiredSeasonNumbers(tvDetails);

        hydrateRequiredSeasons(job, media, requiredSeasons);

        List<ShowEpisode> targetEpisodes = endedShow
            ? showCatalogService.requireAllEligibleEpisodesFromCache(media, tvDetails)
            : showCatalogService.requireAiredEligibleEpisodesFromCache(media, tvDetails);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            UserShowTracking tracking = showTrackingWriteSupport.loadOrCreateTracking(job.getUser(), media);
            showTrackingWriteSupport.addEpisodeWatches(tracking, targetEpisodes, LocalDateTime.now());
            showTrackingWriteSupport.applyCalculationAndPersist(
                tracking,
                media,
                eligibleEpisodesFromCache(media),
                airedEligibleEpisodesFromCache(media),
                true,
                endedShow,
                endedShow
            );
        });
    }

    private void processMarkUpToDateJob(ShowTrackingJob job) {
        Media media = job.getMedia();
        TmdbTvDetailsDTO tvDetails = showCatalogService.fetchAndRefreshShowDetails(media.getTmdbId(), media);
        List<Integer> requiredSeasons = showCatalogService.getRequiredAiredSeasonNumbers(tvDetails);

        hydrateRequiredSeasons(job, media, requiredSeasons);

        List<ShowEpisode> targetEpisodes = showCatalogService.requireAiredEligibleEpisodesFromCache(media, tvDetails);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            UserShowTracking tracking = showTrackingWriteSupport.loadOrCreateTracking(job.getUser(), media);
            showTrackingWriteSupport.addEpisodeWatches(tracking, targetEpisodes, LocalDateTime.now());
            showTrackingWriteSupport.applyCalculationAndPersist(
                tracking,
                media,
                eligibleEpisodesFromCache(media),
                airedEligibleEpisodesFromCache(media),
                true,
                showCatalogService.isFullMetadataAvailable(media, tvDetails),
                false
            );
        });
    }

    private void processMarkPreviousEpisodesWatchedJob(ShowTrackingJob job) {
        Media media = job.getMedia();
        TmdbTvDetailsDTO tvDetails = showCatalogService.fetchAndRefreshShowDetails(media.getTmdbId(), media);
        List<Integer> requiredSeasons = showCatalogService.getRequiredSeasonNumbersThroughPointer(tvDetails, job.getTargetSeasonNumber());

        hydrateRequiredSeasons(job, media, requiredSeasons);

        List<ShowEpisode> targetEpisodes = showCatalogService.requireEligibleEpisodesThroughPointerFromCache(
            media,
            tvDetails,
            job.getTargetSeasonNumber(),
            job.getTargetEpisodeNumber()
        );

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            UserShowTracking tracking = showTrackingWriteSupport.loadOrCreateTracking(job.getUser(), media);
            tracking.setWatchPositionSeason(job.getTargetSeasonNumber());
            tracking.setWatchPositionEpisode(job.getTargetEpisodeNumber());
            showTrackingWriteSupport.addEpisodeWatches(tracking, targetEpisodes, LocalDateTime.now());
            showTrackingWriteSupport.applyCalculationAndPersist(
                tracking,
                media,
                eligibleEpisodesFromCache(media),
                airedEligibleEpisodesFromCache(media),
                showCatalogService.isAiredMetadataAvailable(media, tvDetails),
                showCatalogService.isFullMetadataAvailable(media, tvDetails),
                showCatalogService.isEndedShow(tvDetails)
            );
        });
    }

    private void hydrateRequiredSeasons(ShowTrackingJob job, Media media, Collection<Integer> requiredSeasons) {
        List<Integer> missingSeasons = showCatalogService.findMissingOrStaleRequiredSeasons(media, requiredSeasons);
        updateJobTotals(job.getId(), requiredSeasons.size(), 0);

        int completed = 0;
        for (Integer seasonNumber : missingSeasons) {
            showCatalogService.ensureSeasonCached(media, media.getTmdbId(), seasonNumber);
            completed++;
            updateJobTotals(job.getId(), requiredSeasons.size(), completed);
        }
    }

    private List<ShowEpisode> eligibleEpisodesFromCache(Media media) {
        return showCatalogService.getAllCachedEpisodes(media.getId()).stream()
            .filter(showCatalogService::isEligibleEpisode)
            .toList();
    }

    private List<ShowEpisode> airedEligibleEpisodesFromCache(Media media) {
        return eligibleEpisodesFromCache(media).stream()
            .filter(showCatalogService::isAiredEpisode)
            .toList();
    }

    protected void updateJobTotals(Long jobId, Integer totalSeasons, Integer completedSeasons) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
            showTrackingJobRepository.findById(jobId).ifPresent(job -> {
                job.setTotalSeasons(totalSeasons);
                job.setCompletedSeasons(completedSeasons);
                job.setUpdatedAt(LocalDateTime.now());
                showTrackingJobRepository.save(job);
            })
        );
    }

    protected void requeueJob(Long jobId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
            showTrackingJobRepository.findById(jobId).ifPresent(job -> {
                job.setStatus(ShowTrackingJobStatus.PENDING);
                job.setStartedAt(null);
                job.setUpdatedAt(LocalDateTime.now());
                showTrackingJobRepository.save(job);
            })
        );
    }

    protected void completeJob(Long jobId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
            showTrackingJobRepository.findById(jobId).ifPresent(job -> {
                job.setStatus(ShowTrackingJobStatus.COMPLETED);
                job.setCompletedAt(LocalDateTime.now());
                job.setUpdatedAt(LocalDateTime.now());
                showTrackingJobRepository.save(job);
            })
        );
    }

    protected void failJob(Long jobId, String errorCode, String errorMessage) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
            showTrackingJobRepository.findById(jobId).ifPresent(job -> {
                job.setStatus(ShowTrackingJobStatus.FAILED);
                job.setErrorCode(errorCode);
                job.setErrorMessage(errorMessage);
                job.setUpdatedAt(LocalDateTime.now());
                showTrackingJobRepository.save(job);
            })
        );
    }

    private ShowTrackingJobDTO toDto(ShowTrackingJob job) {
        WatchStatus finalStatus = null;
        if (job.getUser() != null) {
            finalStatus = userShowTrackingRepository.findByUserAndMedia(job.getUser(), job.getMedia())
                .map(UserShowTracking::getStatus)
                .orElse(null);
        }

        return ShowTrackingJobDTO.builder()
            .jobId(job.getId())
            .status(job.getStatus())
            .jobType(job.getJobType())
            .mediaId(job.getMedia().getId())
            .tmdbId(job.getMedia().getTmdbId())
            .message(messageFor(job))
            .totalSeasons(job.getTotalSeasons())
            .completedSeasons(job.getCompletedSeasons())
            .requestedStatus(job.getRequestedStatus())
            .targetSeasonNumber(job.getTargetSeasonNumber())
            .targetEpisodeNumber(job.getTargetEpisodeNumber())
            .errorCode(job.getErrorCode())
            .errorMessage(job.getErrorMessage())
            .finalStatus(finalStatus)
            .createdAt(job.getCreatedAt())
            .updatedAt(job.getUpdatedAt())
            .startedAt(job.getStartedAt())
            .completedAt(job.getCompletedAt())
            .build();
    }

    private String messageFor(ShowTrackingJob job) {
        return switch (job.getJobType()) {
            case HYDRATE_SHOW_CATALOG -> "WatchMate is hydrating this show's catalog in the background.";
            case MARK_SHOW_WATCHED -> "WatchMate is preparing this show and marking episodes watched.";
            case MARK_SHOW_UP_TO_DATE -> "WatchMate is preparing this show and marking aired episodes watched.";
            case MARK_PREVIOUS_EPISODES_WATCHED -> "WatchMate is preparing this show and marking previous episodes watched.";
        };
    }

    private String resolveErrorCode(RuntimeException ex) {
        if (ex instanceof com.project.watchmate.Exception.TmdbUnavailableException) {
            return "TMDB_UNAVAILABLE";
        }
        if (ex instanceof com.project.watchmate.Exception.ShowMetadataSyncRequiredException) {
            return "SHOW_METADATA_SYNC_REQUIRED";
        }
        if (ex instanceof IllegalArgumentException) {
            return "INVALID_REQUEST";
        }
        return "SHOW_TRACKING_JOB_FAILED";
    }

    private String trimErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Show tracking job failed.";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
