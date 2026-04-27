package com.project.watchmate.Dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "Review", description = "Review data returned by review endpoints.")
public class ReviewResponseDTO {

    @Schema(description = "Username of the reviewer.", example = "cinephile42")
    private String username;

    @Schema(description = "Review text.")
    private String comment;

    @Schema(description = "Star rating from 1 to 5.", example = "5")
    private int starRating;

    @Schema(description = "Internal identifier of the review.", example = "101")
    private Long reviewId;

    @Schema(description = "TMDB identifier of the reviewed media item.", example = "1399")
    private Long tmdbId;

    @Schema(description = "Timestamp when the review was created.")
    private LocalDateTime postedAt;

    @Schema(description = "Timestamp when the review was last updated.")
    private LocalDateTime updatedAt;

}
