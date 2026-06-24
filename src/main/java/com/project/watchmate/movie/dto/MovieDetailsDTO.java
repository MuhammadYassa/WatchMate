package com.project.watchmate.movie.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.watchmate.media.extras.dto.CastMemberDTO;
import com.project.watchmate.media.extras.dto.TrailerDTO;
import com.project.watchmate.media.extras.dto.WatchProvidersDTO;
import com.project.watchmate.review.dto.ReviewResponseDTO;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.catalog.domain.WatchStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "MovieDetails", description = "Detailed movie information for the movie details endpoint.")
public class MovieDetailsDTO {

    @Schema(description = "TMDB identifier for the movie.", example = "550")
    private Long tmdbId;

    @Schema(description = "Movie title.", example = "Fight Club")
    private String title;

    @Schema(description = "Movie overview.")
    private String overview;

    @Schema(description = "Relative TMDB poster path.", example = "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg")
    private String posterPath;

    @Schema(description = "Relative TMDB backdrop path when available.")
    private String backdropPath;

    @Schema(description = "Movie release date.")
    private LocalDate releaseDate;

    @Schema(description = "Average movie rating.", example = "8.4")
    private Double rating;

    @Schema(description = "Media type for this response.")
    private MediaType type;

    @Schema(description = "Resolved genre names for the movie.")
    private List<String> genres;

    @Schema(description = "Reviews currently associated with the movie.")
    private List<ReviewResponseDTO> reviews;

    @Schema(description = "Whether the authenticated user has favourited this movie. Null when unauthenticated.")
    @JsonProperty("isFavourited")
    private Boolean isFavourited;

    @Schema(description = "Watch status of the authenticated user for this movie. Null when unauthenticated.")
    private WatchStatus watchStatus;

    @Schema(description = "Top cast members for this movie, sorted by TMDB billing order. Empty when unavailable.")
    private List<CastMemberDTO> cast;

    @Schema(description = "Best available YouTube trailer for this movie. Null when no suitable video exists.")
    private TrailerDTO bestTrailer;

    @Schema(description = "Streaming, rental, purchase, ad-supported, and free provider availability for the configured region.")
    private WatchProvidersDTO watchProviders;
}




