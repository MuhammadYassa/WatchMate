package com.project.watchmate.Dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TmdbMovieDTO {

    private Long id;

    private String title;

    private String overview;

    @JsonProperty("media_type")
    private String mediaType;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("release_date")
    private String releaseDate;

    @JsonProperty("vote_average")
    private double voteAverage;

    @Builder.Default
    @JsonProperty("genres")
    private List<TmdbGenreDTO> genres = new ArrayList<>();

    @Builder.Default
    @JsonProperty("genre_ids")
    private List<Long> genreIds = new ArrayList<>();

    @JsonProperty("name")
    private void mapName(String name) {
        if (title == null || title.isBlank()) {
            title = name;
        }
    }

    @JsonProperty("first_air_date")
    private void mapFirstAirDate(String airDate) {
        if (releaseDate == null || releaseDate.isBlank()) {
            releaseDate = airDate;
        }
    }

    public static Optional<LocalDate> parseDate(String dateString){
        if (dateString == null || dateString.isBlank()){
            return null;
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
