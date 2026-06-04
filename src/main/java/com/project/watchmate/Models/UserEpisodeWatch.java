package com.project.watchmate.Models;

import java.time.LocalDateTime;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_episode_progress", uniqueConstraints = @UniqueConstraint(
    name = "uq_user_episode_progress_progress_season_episode",
    columnNames = {"user_show_progress_id", "season_number", "episode_number"}
))
@BatchSize(size = 50)
public class UserEpisodeWatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_show_progress_id", nullable = false)
    private UserShowTracking userShowTracking;

    private Integer seasonNumber;

    private Integer episodeNumber;

    private LocalDateTime watchedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @jakarta.persistence.PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = watchedAt == null ? now : watchedAt;
        }
        if (updatedAt == null) {
            updatedAt = watchedAt == null ? now : watchedAt;
        }
    }

    @jakarta.persistence.PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
