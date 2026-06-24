package com.project.watchmate.media.tmdb.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbVideosResponseDTO {

    private Long id;

    @Builder.Default
    private List<TmdbVideoDTO> results = new ArrayList<>();
}
