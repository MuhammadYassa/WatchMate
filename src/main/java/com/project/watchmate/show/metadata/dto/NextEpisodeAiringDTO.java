package com.project.watchmate.show.metadata.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "NextEpisodeAiring", description = "Next episode airing metadata for a show.")
public class NextEpisodeAiringDTO {

    private Long tmdbId;

    private LocalDate nextEpisodeAirDate;

    private Integer seasonNumber;

    private Integer episodeNumber;

    private String episodeName;

    private Integer lastEpisodeToAirSeasonNumber;

    private Integer lastEpisodeToAirEpisodeNumber;

    private String lastEpisodeToAirName;
}


