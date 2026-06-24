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
public class TmdbCastMemberDTO {

    private Long id;
    private String name;
    private String character;

    @JsonProperty("profile_path")
    private String profilePath;

    private Integer order;

    @JsonProperty("known_for_department")
    private String knownForDepartment;
}
