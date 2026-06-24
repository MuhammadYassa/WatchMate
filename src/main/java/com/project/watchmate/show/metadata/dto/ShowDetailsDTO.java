package com.project.watchmate.show.metadata.dto;

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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ShowDetails", description = "Detailed show information including season summaries.")
public class ShowDetailsDTO {

    @Schema(description = "TMDB identifier for the show.", example = "1399")
    private Long tmdbId;

    @Schema(description = "Media type for this response.")
    private MediaType type;

    @Schema(description = "Show title.")
    private String title;

    @Schema(description = "Show overview.")
    private String overview;

    @Schema(description = "Relative TMDB poster path.")
    private String posterPath;

    @Schema(description = "Relative TMDB backdrop path when available.")
    private String backdropPath;

    @Schema(description = "First air date for the show.")
    private LocalDate firstAirDate;

    @Schema(description = "Average rating for the show.", example = "8.7")
    private Double rating;

    @Schema(description = "Resolved genre names for the show.")
    private List<String> genres;

    @Schema(description = "Reviews currently associated with the show.")
    private List<ReviewResponseDTO> reviews;

    @Schema(description = "Number of seasons for the show.")
    private Integer numberOfSeasons;

    @Schema(description = "Number of episodes for the show.")
    private Integer numberOfEpisodes;

    @Schema(description = "Last air date for the show.")
    private LocalDate lastAirDate;

    @Schema(description = "TMDB status string for the show.")
    private String tmdbShowStatus;

    @Schema(description = "Next episode air date for the show.")
    private LocalDate nextEpisodeAirDate;

    @Schema(description = "Next episode season number.")
    private Integer nextEpisodeSeasonNumber;

    @Schema(description = "Next episode number.")
    private Integer nextEpisodeEpisodeNumber;

    @Schema(description = "Next episode title.")
    private String nextEpisodeName;

    @Schema(description = "Last aired episode season number.")
    private Integer lastEpisodeToAirSeasonNumber;

    @Schema(description = "Last aired episode number.")
    private Integer lastEpisodeToAirEpisodeNumber;

    @Schema(description = "Last aired episode title.")
    private String lastEpisodeToAirName;

    @Schema(description = "Season summaries for the show.")
    private List<ShowSeasonSummaryDTO> seasons;

    @Schema(description = "Whether the authenticated user has favourited this show. Null when unauthenticated.")
    @JsonProperty("isFavourited")
    private Boolean isFavourited;

    @Schema(description = "Watch status of the authenticated user for this show. Null when unauthenticated.")
    private WatchStatus watchStatus;

    @Schema(description = "Top cast members for this show, sorted by TMDB billing order. Empty when unavailable.")
    private List<CastMemberDTO> cast;

    @Schema(description = "Best available YouTube trailer for this show. Null when no suitable video exists.")
    private TrailerDTO bestTrailer;

    @Schema(description = "Streaming, rental, purchase, ad-supported, and free provider availability for the configured region.")
    private WatchProvidersDTO watchProviders;
}




