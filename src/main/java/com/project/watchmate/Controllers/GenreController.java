package com.project.watchmate.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Dto.ApiError;
import com.project.watchmate.Dto.GenreBrowseResponseDTO;
import com.project.watchmate.Services.GenreBrowseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/genre")
@Tag(name = "Genre", description = "Public genre browse endpoints.")
public class GenreController {

    private static final int MAX_SIZE = 20;

    private final GenreBrowseService genreBrowseService;

    @GetMapping("/{genre}/movies")
    @Operation(summary = "Browse movies by genre", description = "Resolves a local genre name and fetches TMDB movie discover results.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Genre movie results returned", content = @Content(schema = @Schema(implementation = GenreBrowseResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Genre not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<GenreBrowseResponseDTO> browseMovies(
        @PathVariable String genre,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_SIZE) int size
    ) {
        return ResponseEntity.ok(genreBrowseService.browseMovies(genre, page, size));
    }

    @GetMapping("/{genre}/shows")
    @Operation(summary = "Browse shows by genre", description = "Resolves a local genre name and fetches TMDB TV discover results.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Genre show results returned", content = @Content(schema = @Schema(implementation = GenreBrowseResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Genre not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<GenreBrowseResponseDTO> browseShows(
        @PathVariable String genre,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_SIZE) int size
    ) {
        return ResponseEntity.ok(genreBrowseService.browseShows(genre, page, size));
    }
}
