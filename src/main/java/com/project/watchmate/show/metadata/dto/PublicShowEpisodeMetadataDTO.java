package com.project.watchmate.show.metadata.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicShowEpisodeMetadataDTO {

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
