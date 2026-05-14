package com.project.watchmate.Dto;

import java.util.List;

import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.WatchStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ShowProgress", description = "Show progress summary for the authenticated user.")
public class ShowProgressDTO {

    private Long tmdbId;

    private MediaType type;

    private WatchStatus status;

    private Integer currentSeasonNumber;

    private Integer currentEpisodeNumber;

    private Integer watchPositionSeason;

    private Integer watchPositionEpisode;

    private Integer episodesWatchedCount;

    private Integer seasonsCompletedCount;

    private Boolean completed;

    private List<EpisodeProgressDTO> watchedEpisodes;
}
