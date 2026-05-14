package com.project.watchmate.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "MarkSeasonRequest", description = "Payload used to mark every episode in a season watched or unwatched.")
public record MarkSeasonRequest(
    @NotNull
    @Schema(description = "Whether the full season should be marked watched.", example = "true")
    Boolean watched
) {
}
