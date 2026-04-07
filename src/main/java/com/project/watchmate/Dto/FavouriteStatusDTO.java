package com.project.watchmate.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "FavouriteStatus", description = "Favourite status for a specific media item.")
public class FavouriteStatusDTO {

    @Schema(description = "TMDB identifier for the media item.", example = "550")
    private Long tmdbId;

    @Schema(description = "Whether the media item is favourited by the authenticated user.")
    private boolean isFavourited;
    
}
