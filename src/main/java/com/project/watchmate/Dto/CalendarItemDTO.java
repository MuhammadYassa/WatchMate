package com.project.watchmate.Dto;

import java.time.LocalDate;

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
@Schema(name = "CalendarItem", description = "One calendar row item for the authenticated user.")
public class CalendarItemDTO {

    @Schema(description = "Episode air date from the local show snapshot.")
    private LocalDate airDate;

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
    private Integer seasonNumber;

    @Schema(description = "Next episode number when available locally.", example = "5")
    private Integer episodeNumber;

    @Schema(description = "Next episode title when available locally.", example = "Chapter Five")
    private String episodeTitle;

    @Schema(description = "TMDB show lifecycle status when available locally.", example = "Returning Series")
    private String showStatus;

    @Schema(description = "Current local watch status for this tracked show.")
    private WatchStatus watchStatus;
}
