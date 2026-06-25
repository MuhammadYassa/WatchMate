package com.project.watchmate.dashboard.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.catalog.domain.WatchStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ToWatchItem", description = "One item from the authenticated user's to-watch list.")
public class ToWatchItemDTO {

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

    @Schema(description = "Average rating when available.", example = "8.4")
    private Double rating;

    @Schema(description = "Watch status — always TO_WATCH for items returned by this endpoint.")
    private WatchStatus watchStatus;

    @Schema(description = "Release date for movies. Null for shows.")
    private LocalDate releaseDate;

    @Schema(description = "First air date for shows. Null for movies.")
    private LocalDate firstAirDate;

    @Schema(description = "Timestamp of the most recent status update.")
    private LocalDateTime updatedAt;
}
