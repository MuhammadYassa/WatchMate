package com.project.watchmate.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "CreateReviewRequest", description = "Payload used to create a review.")
public class CreateReviewRequestDTO {

    @Size(max=1000)
    @Schema(description = "Optional review text.", example = "Great pacing and memorable performances.", maxLength = 1000)
    private String comment;

    @Min(1)
    @Max(5)
    @Schema(description = "Star rating from 1 to 5.", example = "5", minimum = "1", maximum = "5")
    private int starRating;

    @NotNull
    @Schema(description = "Internal media identifier for the reviewed item.", example = "42")
    private Long mediaId;

}
