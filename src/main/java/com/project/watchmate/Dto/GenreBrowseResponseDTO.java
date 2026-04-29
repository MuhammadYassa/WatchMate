package com.project.watchmate.Dto;

import java.util.List;

import com.project.watchmate.Models.MediaType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "GenreBrowseResponse", description = "Paginated genre browse response.")
public class GenreBrowseResponseDTO {

    @Schema(description = "Resolved local genre name.", example = "Action")
    private String genre;

    @Schema(description = "Media type represented by the results.", example = "MOVIE")
    private MediaType mediaType;

    @Schema(description = "Results for the requested page.")
    private List<DiscoveryMediaItemDTO> results;

    @Schema(description = "Current page number.", example = "1")
    private int currentPage;

    @Schema(description = "Total number of pages available.", example = "100")
    private int totalPages;

    @Schema(description = "Total number of results available.", example = "2000")
    private int totalResults;
}
