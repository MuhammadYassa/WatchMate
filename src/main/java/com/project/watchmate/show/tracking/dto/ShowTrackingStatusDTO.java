package com.project.watchmate.show.tracking.dto;

import com.project.watchmate.media.catalog.domain.WatchStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ShowTrackingStatus", description = "Canonical show tracking status response.")
public class ShowTrackingStatusDTO {

    @Schema(description = "TMDB identifier of the show.", example = "1399")
    private Long tmdbId;

    @Schema(description = "Canonical stored show status. NONE is returned when no tracking row exists, but it is not stored.")
    private WatchStatus status;
}



