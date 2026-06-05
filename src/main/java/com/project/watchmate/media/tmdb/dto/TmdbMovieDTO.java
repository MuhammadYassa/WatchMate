package com.project.watchmate.media.tmdb.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbMovieDTO {

    private Long id;

    @JsonAlias("name")
    private String title;

    private String overview;

    @JsonProperty("media_type")
    private String mediaType;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("backdrop_path")
    private String backdropPath;

    @JsonProperty("release_date")
    @JsonAlias("first_air_date")
    private String releaseDate;

    @JsonProperty("vote_average")
    private Double voteAverage;

    @Builder.Default
    @JsonProperty("genres")
    private List<TmdbGenreDTO> genres = new ArrayList<>();

    @Builder.Default
    @JsonProperty("genre_ids")
    private List<Long> genreIds = new ArrayList<>();

    public static Optional<LocalDate> parseDate(String dateString){
        if (dateString == null || dateString.isBlank()){
            return Optional.empty();
        }
        try{
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return Optional.of(LocalDate.parse(dateString, dateTimeFormatter));
        }
        catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}


