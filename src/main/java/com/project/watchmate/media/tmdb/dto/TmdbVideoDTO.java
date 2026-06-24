package com.project.watchmate.media.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbVideoDTO {

    private String key;
    private String name;
    private String site;
    private String type;
    private Boolean official;

    @JsonProperty("published_at")
    private String publishedAt;
}
