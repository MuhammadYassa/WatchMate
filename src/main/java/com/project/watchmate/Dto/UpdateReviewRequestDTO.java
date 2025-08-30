package com.project.watchmate.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateReviewRequestDTO {

    private String comment;

    private int starRating;

    private Long reviewId;

}
