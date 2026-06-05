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
public class TmdbTvSeasonDTO {

    private Long id;

    private String name;

    private String overview;

    @JsonProperty("air_date")
    private String airDate;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("season_number")
    private Integer seasonNumber;

    @JsonProperty("episode_count")
    private Integer episodeCount;

    @JsonProperty("episodes")
    @Builder.Default
    private List<TmdbTvEpisodeDTO> episodes = new ArrayList<>();
}


