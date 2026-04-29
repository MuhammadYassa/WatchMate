package com.project.watchmate.Dto;

import java.time.LocalDate;

import com.project.watchmate.Models.MediaType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "DiscoveryMediaItem", description = "Media item returned by homepage and discovery feed endpoints.")
public class DiscoveryMediaItemDTO {

    @Schema(description = "TMDB identifier for the media item.", example = "550")
    private Long tmdbId;

    @Schema(description = "Display title for the media item.", example = "Fight Club")
    private String title;

    @Schema(description = "Short overview of the media item.")
    private String overview;

    @Schema(description = "Relative poster path.", example = "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg")
    private String posterPath;

    @Schema(description = "Relative backdrop path.", example = "/52AfXWuXCHn3UjD17rBruA9f5qb.jpg")
    private String backdropPath;

    @Schema(description = "Primary release date or first air date.")
    private LocalDate releaseDate;

    @Schema(description = "Average rating for the media item.", example = "8.4")
    private Double rating;

    @Schema(description = "Media type.", example = "MOVIE")
    private MediaType type;
}
