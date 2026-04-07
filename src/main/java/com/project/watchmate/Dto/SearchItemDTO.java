package com.project.watchmate.Dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SearchItem", description = "Search result item returned by media search.")
public class SearchItemDTO {

    @Schema(description = "TMDB identifier for the result.", example = "550")
    private Long id;

    @Schema(description = "Display title of the result.", example = "Fight Club")
    private String title;

    @Schema(description = "Short overview of the result.")
    private String overview;

    @Schema(description = "Media type string from TMDB.", example = "movie")
    private String mediaType;

    @Schema(description = "Relative poster path.", example = "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg")
    private String posterPath;

    @Schema(description = "Release date string returned by TMDB.", example = "1999-10-15")
    private String releaseDate;

    @Schema(description = "Average vote rating.", example = "8.4")
    private Double voteAverage;

    @Builder.Default
    @Schema(description = "Resolved genre names for the result.")
    private List<String> genres = new ArrayList<>();

}
