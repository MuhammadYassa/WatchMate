package com.project.watchmate.watchlist.dto;

import com.project.watchmate.media.catalog.dto.MediaDetailsDTO;

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
@Schema(name = "WatchList", description = "Watchlist summary returned by watchlist endpoints.")
public class WatchListDTO {

    @Schema(description = "Internal identifier of the watchlist.", example = "12")
    private Long id;

    @Schema(description = "Display name of the watchlist.", example = "Weekend Movies")
    private String name;

    @Schema(description = "Media items currently in the watchlist.")
    private List<MediaDetailsDTO> media;
    
}


