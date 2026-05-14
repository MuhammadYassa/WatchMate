package com.project.watchmate.Dto;

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
@Schema(name = "ShowSeasonSummary", description = "Season summary for a public show details response.")
public class ShowSeasonSummaryDTO {

    private Long tmdbSeasonId;

    private Integer seasonNumber;

    private String name;

    private String overview;

    private LocalDate airDate;

    private Integer episodeCount;

    private String posterPath;
}
