package com.project.watchmate.show.metadata.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ShowSeasonsDetails", description = "Public TV season details including episodes for one requested season.")
public class ShowSeasonsDetailsDTO {

    private Long tmdbId;

    private Integer seasonNumber;

    private String name;

    private String overview;

    private String posterPath;

    private LocalDate airDate;

    private Integer episodeCount;

    private List<ShowEpisodeDetailsDTO> episodes;
}


