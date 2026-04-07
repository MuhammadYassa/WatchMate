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
@Schema(name = "UserFavourites", description = "Favourite media collection for the authenticated user.")
public class UserFavouritesDTO {

    @Schema(description = "Favourite media items.")
    private List<MediaDetailsDTO> favourites;

    @Schema(description = "Total number of favourites returned.", example = "8")
    private int totalCount;
}
