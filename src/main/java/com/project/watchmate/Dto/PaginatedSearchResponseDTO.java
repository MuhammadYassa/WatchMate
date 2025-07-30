package com.project.watchmate.Dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedSearchResponseDTO {

    private List<SearchItemDTO> searchResults;

    private int currentPage;

    private int totalPages;
    
    private int totalResults;
}
