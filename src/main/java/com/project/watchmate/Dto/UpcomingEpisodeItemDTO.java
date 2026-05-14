package com.project.watchmate.Dto;

import java.time.LocalDate;

import com.project.watchmate.Models.MediaType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "UpcomingEpisodeItem", description = "One upcoming-episode row item for the authenticated user.")
public class UpcomingEpisodeItemDTO {

    @Schema(description = "TMDB identifier of the show.", example = "1399")
    private Long tmdbId;

    @Schema(description = "Media type.")
    private MediaType type;

    @Schema(description = "Display title.", example = "Game of Thrones")
    private String title;

    @Schema(description = "Poster image path.", example = "/poster.jpg")
    private String posterPath;

    @Schema(description = "Backdrop image path when available.", example = "/backdrop.jpg")
    private String backdropPath;

    @Schema(description = "Next episode season number when available locally.", example = "2")
    private Integer nextEpisodeSeasonNumber;

    @Schema(description = "Next episode number when available locally.", example = "5")
    private Integer nextEpisodeEpisodeNumber;

    @Schema(description = "Next episode title when available locally.", example = "Chapter Five")
    private String nextEpisodeName;

    @Schema(description = "Local snapshot of the next episode air date.")
    private LocalDate nextEpisodeAirDate;

    @Schema(description = "Days until the episode air date, based on the server's local date.", example = "3")
    private Long daysUntilAirDate;

    @Schema(description = "TMDB show lifecycle status when available locally.", example = "Returning Series")
    private String tmdbShowStatus;
}
