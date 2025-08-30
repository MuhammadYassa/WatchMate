package com.project.watchmate.Dto;

import java.time.LocalDate;
import java.util.List;

import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.WatchStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MediaDetailsDTO {
    
    private Long tmdbId;

    private String title;

    private String overview;

    private String posterPath;

    private LocalDate releaseDate;
    
    private Double rating;

    private MediaType type;

    private List<String> genres;

    private List<ReviewResponseDTO> reviews;

    private boolean isFavourited;

    private WatchStatus watchStatus;
}
