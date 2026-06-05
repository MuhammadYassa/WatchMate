package com.project.watchmate.media.search.api;

import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.common.error.ApiError;
import com.project.watchmate.media.search.dto.PaginatedSearchResponseDTO;
import com.project.watchmate.media.search.application.SearchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Validated
@Tag(name = "Media", description = "Media discovery endpoints.")
public class MediaController {

    private final SearchService searchService;

    @GetMapping("/search")
    @Operation(summary = "Search media", description = "Searches movies and shows by query text.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search results returned", content = @Content(schema = @Schema(implementation = PaginatedSearchResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid query parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PaginatedSearchResponseDTO> getSearchResponse(
        @RequestParam @NotBlank String query,
        @RequestParam(defaultValue = "1") @Min(1) int page
    ) {
        PaginatedSearchResponseDTO dto = searchService.search(query, page);
        return ResponseEntity.ok(dto);
    }
}




