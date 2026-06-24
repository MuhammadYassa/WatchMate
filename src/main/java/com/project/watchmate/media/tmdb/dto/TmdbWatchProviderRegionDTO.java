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
public class TmdbWatchProviderRegionDTO {

    private String link;

    @Builder.Default
    private List<TmdbWatchProviderEntryDTO> flatrate = new ArrayList<>();

    @Builder.Default
    private List<TmdbWatchProviderEntryDTO> rent = new ArrayList<>();

    @Builder.Default
    private List<TmdbWatchProviderEntryDTO> buy = new ArrayList<>();

    @Builder.Default
    private List<TmdbWatchProviderEntryDTO> ads = new ArrayList<>();

    @Builder.Default
    private List<TmdbWatchProviderEntryDTO> free = new ArrayList<>();
}
