package com.project.watchmate.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "PopularMedia", description = "Popular media item returned by the popular media endpoint.")
public class PopularMediaResponseDTO {
        @Schema(description = "Popularity rank in the current response.", example = "1") 
        private int rank;

        @Schema(description = "Title of the media item.", example = "The Dark Knight") 
        private String title;

        @Schema(description = "Short overview of the media item.") 
        private String overview;

        @Schema(description = "Relative poster path.", example = "/qJ2tW6WMUDux911r6m7haRef0WH.jpg") 
        private String posterPath;

        @Schema(description = "Average rating for the media item.", example = "8.6") 
        private Double rating;

        @Schema(description = "Media type.", example = "MOVIE") 
        private String type;
}
