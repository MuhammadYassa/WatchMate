package com.project.watchmate.Dto;

import java.time.LocalDateTime;

import com.project.watchmate.Models.MediaType;
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
@Schema(name = "ContinueWatchingItem", description = "One continue-watching row item for the authenticated user.")
public class ContinueWatchingItemDTO {

    @Schema(description = "TMDB identifier of the media item.", example = "550")
    private Long tmdbId;

    @Schema(description = "Media type.")
    private MediaType type;

    @Schema(description = "Display title.", example = "Fight Club")
    private String title;

    @Schema(description = "Poster image path.", example = "/poster.jpg")
    private String posterPath;

    @Schema(description = "Backdrop image path when available.", example = "/backdrop.jpg")
    private String backdropPath;

    @Schema(description = "Current user watch status.")
    private WatchStatus watchStatus;

    @Schema(description = "Resume season number for shows. This prefers the saved watch position and otherwise falls back to the latest watched episode.", example = "2")
    private Integer resumeSeasonNumber;

    @Schema(description = "Resume episode number for shows. This prefers the saved watch position and otherwise falls back to the latest watched episode.", example = "5")
    private Integer resumeEpisodeNumber;

    @Schema(description = "Next season number for shows when available locally.", example = "2")
    private Integer nextSeasonNumber;

    @Schema(description = "Next episode number for shows when available locally.", example = "6")
    private Integer nextEpisodeNumber;

    @Schema(description = "Last watched timestamp when available.")
    private LocalDateTime lastWatchedAt;

    @Schema(description = "Most recent local status update timestamp.")
    private LocalDateTime updatedAt;

    @Schema(description = "Average rating when available.", example = "8.4")
    private Double rating;
}
