package com.project.watchmate.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "MarkEpisodeRequest", description = "Payload used to mark a show episode watched or unwatched.")
public record MarkEpisodeRequest(
    @NotNull
    @Schema(description = "Whether the episode should be marked watched.", example = "true")
    Boolean watched
) {
}
