package com.project.watchmate.Dto;

import com.project.watchmate.Models.WatchStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMediaStatusDTO {

	private Long tmdbId;

	private WatchStatus status;

}


