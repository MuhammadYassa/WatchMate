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
@Schema(name = "ShowTracking", description = "Canonical show tracking summary for the authenticated user.")
public class ShowTrackingDTO {

    private Long tmdbId;

    private MediaType type;

    @Schema(description = "Canonical stored show status. When no tracking row exists, the API returns NONE without storing it.")
    private WatchStatus status;

    @Schema(description = "Manual watch position season pointer supplied by the user when available.", example = "2")
    private Integer watchPositionSeason;

    @Schema(description = "Manual watch position episode pointer supplied by the user when available.", example = "5")
    private Integer watchPositionEpisode;

    @Schema(description = "Latest watched season derived from episode watch rows when available.", example = "2")
    private Integer latestWatchedSeason;

    @Schema(description = "Latest watched episode derived from episode watch rows when available.", example = "4")
    private Integer latestWatchedEpisode;

    private Integer episodesWatchedCount;

    private Integer seasonsCompletedCount;

    private Boolean completed;

    private List<WatchedEpisodeDTO> watchedEpisodes;
}
