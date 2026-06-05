package com.project.watchmate.media.tmdb.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TmdbTvDetailsDTO {

    private Long id;

    private String name;

    private String overview;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("backdrop_path")
    private String backdropPath;

    @JsonProperty("first_air_date")
    private String firstAirDate;

    @JsonProperty("vote_average")
    private Double voteAverage;

    @Builder.Default
    @JsonProperty("genres")
    private List<TmdbGenreDTO> genres = new ArrayList<>();

    @JsonProperty("number_of_seasons")
    private Integer numberOfSeasons;

    @JsonProperty("number_of_episodes")
    private Integer numberOfEpisodes;

    @JsonProperty("last_air_date")
    private String lastAirDate;

    private String status;

    @JsonProperty("last_episode_to_air")
    private TmdbEpisodeSummaryDTO lastEpisodeToAir;

    @JsonProperty("next_episode_to_air")
    private TmdbEpisodeSummaryDTO nextEpisodeToAir;

    @Builder.Default
    @JsonProperty("seasons")
    private List<TmdbTvSeasonSummaryDTO> seasons = new ArrayList<>();
}


