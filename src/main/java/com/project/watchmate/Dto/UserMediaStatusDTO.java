package com.project.watchmate.Dto;

import com.project.watchmate.Models.WatchStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "UserMediaStatus", description = "Watch status for a media item for the authenticated user.")
public class UserMediaStatusDTO {

    @Schema(description = "TMDB identifier of the media item.", example = "550")
	private Long tmdbId;

    @Schema(description = "Current watch status.")
	private WatchStatus status;

}


