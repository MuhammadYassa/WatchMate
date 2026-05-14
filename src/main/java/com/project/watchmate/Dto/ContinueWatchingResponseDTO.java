package com.project.watchmate.Dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ContinueWatchingResponse", description = "Continue-watching row payload for the authenticated user.")
public class ContinueWatchingResponseDTO {

    @Builder.Default
    @Schema(description = "Continue-watching items in display order.")
    private List<ContinueWatchingItemDTO> items = List.of();
}
