package com.project.watchmate.Dto;

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
    @Schema(description = "Desired watch status.", example = "WATCHED", allowableValues = {"TO_WATCH", "WATCHING", "WATCHED", "NONE"})
	private String status;

}


