package com.project.watchmate.show.metadata.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicShowSeasonMetadataDTO {

    private Long tmdbId;

    private Integer seasonNumber;

    private String name;

    private String overview;

    private String posterPath;

    private LocalDate airDate;

    private Integer episodeCount;

    private List<PublicShowEpisodeMetadataDTO> episodes;
}
