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
@Schema(name = "UpdateReviewRequest", description = "Payload used to update an existing review.")
public class UpdateReviewRequestDTO {

    @Size(max=1000)
    @NotNull
    @Schema(description = "Updated review text.", example = "Still excellent on rewatch.", maxLength = 1000)
    private String comment;

    @Min(1)
    @Max(5)
    @Schema(description = "Updated star rating from 1 to 5.", example = "4", minimum = "1", maximum = "5")
    private int starRating;

}
