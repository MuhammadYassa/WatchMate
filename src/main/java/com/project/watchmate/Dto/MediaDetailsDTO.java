package com.project.watchmate.Dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.WatchStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "MediaDetails", description = "Detailed media information included in watchlist responses.")
public class MediaDetailsDTO {
    
    @Schema(description = "TMDB identifier for the media.", example = "550")
    private Long tmdbId;

    @Schema(description = "Title of the media item.", example = "Fight Club")
    private String title;

    @Schema(description = "Short overview of the media item.")
    private String overview;

    @Schema(description = "Relative TMDB poster path.", example = "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg")
    private String posterPath;

    @Schema(description = "Primary release date.")
    private LocalDate releaseDate;
    
    @Schema(description = "Average rating for the media item.", example = "8.4")
    private Double rating;

    @Schema(description = "Whether the media item is a movie or show.")
    private MediaType type;

    @Schema(description = "Resolved genre names for the media item.")
    private List<String> genres;

    @Schema(description = "Reviews currently associated with the media item.")
    private List<ReviewResponseDTO> reviews;

    @Schema(description = "Whether the authenticated user has favourited this media item.")
    @JsonProperty("isFavourited")
    private boolean isFavourited;

    @Schema(description = "Watch status of the authenticated user for this media item.")
    private WatchStatus watchStatus;
}
