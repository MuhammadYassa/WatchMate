package com.project.watchmate.Dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "HomeResponse", description = "Homepage discovery payload returned from the local cache.")
public class HomeResponseDTO {

    private List<DiscoveryMediaItemDTO> trendingMovies;

    private List<DiscoveryMediaItemDTO> trendingShows;

    private List<DiscoveryMediaItemDTO> popularNow;

    private List<DiscoveryMediaItemDTO> airingToday;

    private List<DiscoveryMediaItemDTO> upcoming;

    private List<DiscoveryMediaItemDTO> recommendedLater;

    @Schema(description = "Distinct locally cached genre names.")
    private List<String> genres;
}
