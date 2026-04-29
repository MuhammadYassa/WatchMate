package com.project.watchmate.Dto;

import java.time.LocalDateTime;

import com.project.watchmate.Models.ContentSyncResult;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "HomeStatus", description = "Discovery sync status for homepage cache visibility.")
public class HomeStatusDTO {

    private LocalDateTime lastAttemptedAt;

    private LocalDateTime lastSuccessfulAt;

    private LocalDateTime lastFailedAt;

    private ContentSyncResult lastResult;

    private String lastErrorMessage;

    private Integer trendingMoviesCount;

    private Integer trendingShowsCount;

    private Integer popularNowCount;

    private Integer airingTodayCount;

    private Integer upcomingCount;

    private Integer recommendedLaterCount;
}
