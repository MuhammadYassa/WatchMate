package com.project.watchmate.Dto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TmdbMovieDTO {

    private int id;

    private String title;

    private String overview;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("release_date")
    private String releaseDate;

    @JsonProperty("vote_average")
    private double voteAverage;

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

    public static Date parseDate(String dateString){
        if (dateString == null || dateString.isBlank()){
            return null;
        }
        try{
            return new SimpleDateFormat("yyyy-MM-dd").parse(dateString);
        }
        catch (ParseException e) {
            return null;
        }
    }
}
