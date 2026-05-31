package com.project.watchmate.Dto;

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
@Schema(name = "UpdateShowProgressRequest", description = "Payload used to update summary progress for a show.")
public class UpdateShowProgressRequestDTO {

    @Schema(description = "Optional desired status. The server normalizes this to remain consistent with show progress and may return UP_TO_DATE for caught-up ongoing shows.", allowableValues = {"TO_WATCH", "WATCHING", "WATCHED", "UP_TO_DATE", "NONE"})
    private String status;

    @NotNull
    @Min(1)
    @Schema(description = "Current watched-through season number.", example = "2")
    private Integer currentSeasonNumber;

    @NotNull
    @Min(1)
    @Schema(description = "Current watched-through episode number.", example = "5")
    private Integer currentEpisodeNumber;

    @Builder.Default
    @Schema(description = "When true, creates watched episode rows for all prior episodes through the supplied pointer.")
    private Boolean markPreviousEpisodesWatched = Boolean.FALSE;
}
