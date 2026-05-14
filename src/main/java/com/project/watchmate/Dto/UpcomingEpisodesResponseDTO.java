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
@Schema(name = "UpcomingEpisodesResponse", description = "Upcoming episode row payload for the authenticated user.")
public class UpcomingEpisodesResponseDTO {

    @Builder.Default
    @Schema(description = "Upcoming episode items in display order.")
    private List<UpcomingEpisodeItemDTO> items = List.of();
}
