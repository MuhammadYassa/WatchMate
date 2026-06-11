package com.project.watchmate.show.jobs.domain;

import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.WatchStatus;

import java.time.LocalDateTime;

import com.project.watchmate.user.domain.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "show_tracking_job")
public class ShowTrackingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;

    @ManyToOne
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShowTrackingJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShowTrackingJobStatus status;

    @Enumerated(EnumType.STRING)
    @Column
    private WatchStatus requestedStatus;

    @Column(name = "target_season_number")
    private Integer targetSeasonNumber;

    @Column(name = "target_episode_number")
    private Integer targetEpisodeNumber;

    @Column(name = "total_seasons")
    private Integer totalSeasons;

    @Builder.Default
    @Column(name = "completed_seasons", nullable = false)
    private Integer completedSeasons = 0;

    @Column(name = "failed_seasons_json", columnDefinition = "json")
    private String failedSeasonsJson;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @jakarta.persistence.PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (completedSeasons == null) {
            completedSeasons = 0;
        }
        if (attemptCount == null) {
            attemptCount = 0;
        }
    }

    @jakarta.persistence.PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}



