package com.project.watchmate.Models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_show_progress", uniqueConstraints = @UniqueConstraint(
    name = "uq_user_show_progress_user_media",
    columnNames = {"user_id", "media_id"}
))
@Builder
public class UserShowProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    private Integer currentSeasonNumber;

    private Integer currentEpisodeNumber;

    @Column(name = "watch_position_season")
    private Integer watchPositionSeason;

    @Column(name = "watch_position_episode")
    private Integer watchPositionEpisode;

    @Enumerated(EnumType.STRING)
    @Column(name = "tracking_state")
    private ShowTrackingState trackingState;

    @Builder.Default
    @Column(nullable = false)
    private int episodesWatchedCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private int seasonsCompletedCount = 0;

    private LocalDateTime lastWatchedAt;

    @Builder.Default
    @BatchSize(size = 50)
    @OneToMany(mappedBy = "userShowProgress", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserEpisodeProgress> episodeProgress = new ArrayList<>();
}
