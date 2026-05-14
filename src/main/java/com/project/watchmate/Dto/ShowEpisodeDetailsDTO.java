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
@Schema(name = "ShowEpisodeDetails", description = "Episode details for a public show season response.")
public class ShowEpisodeDetailsDTO {

    private Long tmdbEpisodeId;

    private Integer seasonNumber;

    private Integer episodeNumber;

    private String name;

    private String overview;

    private LocalDate airDate;

    private Integer runtime;

    private String stillPath;

    private Boolean isAired;
}
