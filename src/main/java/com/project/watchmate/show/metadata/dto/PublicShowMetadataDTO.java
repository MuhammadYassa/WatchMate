package com.project.watchmate.show.metadata.dto;

import java.time.LocalDate;
import java.util.List;

import com.project.watchmate.media.catalog.domain.MediaType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicShowMetadataDTO {

    private Long tmdbId;

    private MediaType type;

    private String title;

    private String overview;

    private String posterPath;

    private String backdropPath;

    private LocalDate firstAirDate;

    private Double rating;

    private List<String> genres;

    private Integer numberOfSeasons;

    private Integer numberOfEpisodes;

    private LocalDate lastAirDate;

    private String tmdbShowStatus;

    private LocalDate nextEpisodeAirDate;

    private Integer nextEpisodeSeasonNumber;

    private Integer nextEpisodeEpisodeNumber;

    private String nextEpisodeName;

    private Integer lastEpisodeToAirSeasonNumber;

    private Integer lastEpisodeToAirEpisodeNumber;

    private String lastEpisodeToAirName;

    private List<ShowSeasonSummaryDTO> seasons;
}
