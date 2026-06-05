package com.project.watchmate.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "UpdateWatchStatusRequest", description = "Payload used to update a user's watch status for a media item.")
public class UpdateWatchStatusRequestDTO {

	@NotBlank
    @Schema(description = "Desired watch status. Shows additionally accept UP_TO_DATE.", example = "WATCHED", allowableValues = {"TO_WATCH", "WATCHING", "WATCHED", "UP_TO_DATE", "NONE"})
	private String status;

}




