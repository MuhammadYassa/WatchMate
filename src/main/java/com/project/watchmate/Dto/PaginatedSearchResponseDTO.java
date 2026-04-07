package com.project.watchmate.Dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PaginatedSearchResponse", description = "Paginated search result payload.")
public class PaginatedSearchResponseDTO {

    @Schema(description = "Search results for the current page.")
    private List<SearchItemDTO> searchResults;

    @Schema(description = "Current page number.", example = "1")
    private int currentPage;

    @Schema(description = "Total number of available pages.", example = "12")
    private int totalPages;
    
    @Schema(description = "Total number of matching search results.", example = "230")
    private int totalResults;
}
