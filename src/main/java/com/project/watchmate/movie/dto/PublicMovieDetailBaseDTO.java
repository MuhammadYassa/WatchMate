package com.project.watchmate.movie.dto;

import java.time.LocalDate;
import java.util.List;

import com.project.watchmate.media.catalog.domain.MediaType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicMovieDetailBaseDTO {

    private Long tmdbId;

    private String title;

    private String overview;

    private String posterPath;

    private String backdropPath;

    private LocalDate releaseDate;

    private Double rating;

    private MediaType type;

    private List<String> genres;
}
