package com.project.watchmate.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateWatchStatusRequestDTO {

	@NotNull
	@Min(1)
	private Long tmdbId;

	@NotBlank
	private String status;

}


