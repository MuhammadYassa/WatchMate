package com.project.watchmate.media.tmdb.dto;

import java.util.HashMap;
import java.util.Map;

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
public class TmdbWatchProvidersResponseDTO {

    private Long id;

    @Builder.Default
    private Map<String, TmdbWatchProviderRegionDTO> results = new HashMap<>();
}
