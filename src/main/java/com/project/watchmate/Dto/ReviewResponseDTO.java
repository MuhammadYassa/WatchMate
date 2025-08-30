package com.project.watchmate.Dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDTO {

    private String username;

    private String comment;

    private int starRating;

    private Long reviewId;

    private Long mediaId;

    private LocalDateTime postedAt;

    private LocalDateTime updatedAt;

}
