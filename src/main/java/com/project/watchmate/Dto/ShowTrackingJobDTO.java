package com.project.watchmate.Dto;

import java.time.LocalDateTime;

import com.project.watchmate.Models.ShowTrackingJobStatus;
import com.project.watchmate.Models.ShowTrackingJobType;
import com.project.watchmate.Models.WatchStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ShowTrackingJob", description = "Background job state for show catalog hydration or bulk tracking backfill.")
public class ShowTrackingJobDTO {

    private Long jobId;

    private ShowTrackingJobStatus status;

    private ShowTrackingJobType jobType;

    private Long mediaId;

    private Long tmdbId;

    private String message;

    private Integer totalSeasons;

    private Integer completedSeasons;

    private WatchStatus requestedStatus;

    private Integer targetSeasonNumber;

    private Integer targetEpisodeNumber;

    private String errorCode;

    private String errorMessage;

    private WatchStatus finalStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}
