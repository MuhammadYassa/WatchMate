package com.project.watchmate.dashboard.dto;

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
@Schema(name = "CalendarResponse", description = "Calendar row payload for the authenticated user.")
public class CalendarResponseDTO {

    @Builder.Default
    @Schema(description = "Calendar items in display order.")
    private List<CalendarItemDTO> items = List.of();
}


