package com.project.watchmate.show.tracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    name = "UpdateShowTrackingPositionRequest",
    description = "Payload used to set the manual watch position for a tracked show."
)
public class UpdateShowTrackingPositionRequestDTO {

    @NotNull
    @Min(1)
    @Schema(description = "Manual watch position season number.", example = "2")
    private Integer watchPositionSeason;

    @NotNull
    @Min(1)
    @Schema(description = "Manual watch position episode number.", example = "5")
    private Integer watchPositionEpisode;

    @Builder.Default
    @Schema(description = "When true, creates watched episode rows for cached prior episodes through the supplied watch position.")
    private Boolean markPreviousEpisodesWatched = Boolean.FALSE;
}


