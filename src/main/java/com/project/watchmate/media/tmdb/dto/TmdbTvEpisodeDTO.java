package com.project.watchmate.media.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TmdbTvEpisodeDTO {

    private Long id;

    private String name;

    private String overview;

    @JsonProperty("air_date")
    private String airDate;

    @JsonProperty("episode_number")
    private Integer episodeNumber;

    @JsonProperty("season_number")
    private Integer seasonNumber;

    private Integer runtime;

    @JsonProperty("still_path")
    private String stillPath;
}


