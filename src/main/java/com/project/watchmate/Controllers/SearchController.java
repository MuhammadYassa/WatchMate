package com.project.watchmate.Controllers;

import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Dto.PaginatedSearchResponseDTO;
import com.project.watchmate.Services.SearchService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/api/search")
    public ResponseEntity<PaginatedSearchResponseDTO> getSearchResponse(@RequestParam("query") String query, @RequestParam(value = "page", defaultValue = "1") int page) {
        PaginatedSearchResponseDTO dto = searchService.search(query, page);
        return ResponseEntity.ok(dto);
    }
    
}
