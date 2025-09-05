package com.project.watchmate.Dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFavouritesDTO {

    private List<MediaDetailsDTO> favourites;

    private int totalCount;
}
