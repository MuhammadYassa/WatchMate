package com.project.watchmate.show.jobs.persistence;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.show.jobs.domain.ShowTrackingJob;
import com.project.watchmate.show.jobs.domain.ShowTrackingJobStatus;
import com.project.watchmate.show.jobs.domain.ShowTrackingJobType;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;

public interface ShowTrackingJobRepository extends JpaRepository<ShowTrackingJob, Long> {

    @Query("select job from ShowTrackingJob job " +
        "where job.status in :statuses " +
        "and job.media = :media " +
        "and ((:user is null and job.user is null) or job.user = :user) " +
        "and job.jobType = :jobType " +
        "and ((:requestedStatus is null and job.requestedStatus is null) or job.requestedStatus = :requestedStatus) " +
        "and ((:targetSeasonNumber is null and job.targetSeasonNumber is null) or job.targetSeasonNumber = :targetSeasonNumber) " +
        "and ((:targetEpisodeNumber is null and job.targetEpisodeNumber is null) or job.targetEpisodeNumber = :targetEpisodeNumber) " +
        "order by job.createdAt asc")
    List<ShowTrackingJob> findMatchingActiveJobs(
        @Param("user") Users user,
        @Param("media") Media media,
        @Param("jobType") ShowTrackingJobType jobType,
        @Param("requestedStatus") WatchStatus requestedStatus,
        @Param("targetSeasonNumber") Integer targetSeasonNumber,
        @Param("targetEpisodeNumber") Integer targetEpisodeNumber,
        @Param("statuses") Collection<ShowTrackingJobStatus> statuses,
        Pageable pageable
    );

    List<ShowTrackingJob> findByStatusOrderByCreatedAtAsc(ShowTrackingJobStatus status, Pageable pageable);

    List<ShowTrackingJob> findByStatusAndStartedAtBeforeOrderByStartedAtAsc(
        ShowTrackingJobStatus status,
        LocalDateTime startedAtBefore
    );

    @Modifying
    @Query("update ShowTrackingJob job " +
        "set job.status = :runningStatus, " +
        "job.startedAt = :startedAt, " +
        "job.updatedAt = :updatedAt, " +
        "job.attemptCount = job.attemptCount + 1, " +
        "job.errorCode = null, " +
        "job.errorMessage = null " +
        "where job.id = :jobId and job.status = :pendingStatus")
    int claimPendingJob(
        @Param("jobId") Long jobId,
        @Param("pendingStatus") ShowTrackingJobStatus pendingStatus,
        @Param("runningStatus") ShowTrackingJobStatus runningStatus,
        @Param("startedAt") LocalDateTime startedAt,
        @Param("updatedAt") LocalDateTime updatedAt
    );

    @Query("select job from ShowTrackingJob job where job.id = :jobId and job.user = :user")
    Optional<ShowTrackingJob> findVisibleUserJob(@Param("jobId") Long jobId, @Param("user") Users user);
}




