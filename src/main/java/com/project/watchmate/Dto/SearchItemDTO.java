package com.project.watchmate.Dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchItemDTO {

    private Long id;

    private String title;

    private String overview;

    private String mediaType;

    private String posterPath;

    private String releaseDate;

    private double voteAverage;

    @Builder.Default
    private List<String> genres = new ArrayList<>();

}
