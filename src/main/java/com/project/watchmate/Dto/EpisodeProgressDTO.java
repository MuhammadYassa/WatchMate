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
@Schema(name = "EpisodeProgress", description = "Watched episode progress for a show.")
public class EpisodeProgressDTO {

    private Integer seasonNumber;

    private Integer episodeNumber;

    private LocalDateTime watchedAt;
}
