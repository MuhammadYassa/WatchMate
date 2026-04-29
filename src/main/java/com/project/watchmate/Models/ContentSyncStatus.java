package com.project.watchmate.Models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "content_sync_status")
public class ContentSyncStatus {

    @Id
    @Column(name = "status_key", nullable = false)
    private String statusKey;

    private LocalDateTime lastAttemptedAt;

    private LocalDateTime lastSuccessfulAt;

    private LocalDateTime lastFailedAt;

    @Column(length = 1000)
    private String lastErrorMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentSyncResult lastResult;

    private Integer trendingMoviesCount;

    private Integer trendingShowsCount;

    private Integer popularNowCount;

    private Integer airingTodayCount;

    private Integer upcomingCount;

    private Integer recommendedLaterCount;
}
