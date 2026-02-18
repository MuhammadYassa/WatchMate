package com.project.watchmate.Dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReviewRequestDTO {

    @Size(max=1000)
    private String comment;

    @Min(1)
    @Max(5)
    private int starRating;

    @NotNull
    private Long mediaId;

}
