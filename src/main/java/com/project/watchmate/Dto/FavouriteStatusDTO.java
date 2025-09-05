package com.project.watchmate.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FavouriteStatusDTO {

    private Long tmdbId;

    private boolean isFavourited;
    
}
