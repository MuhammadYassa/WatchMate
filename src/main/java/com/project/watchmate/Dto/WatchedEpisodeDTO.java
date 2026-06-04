package com.project.watchmate.Dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "WatchedEpisode", description = "One watched episode row for a tracked show.")
public class WatchedEpisodeDTO {

    private Integer seasonNumber;

    private Integer episodeNumber;

    private LocalDateTime watchedAt;
}
