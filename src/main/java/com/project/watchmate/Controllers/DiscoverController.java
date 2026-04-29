package com.project.watchmate.Controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Dto.ApiError;
import com.project.watchmate.Dto.DiscoveryMediaItemDTO;
import com.project.watchmate.Services.DiscoverService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/discover")
@Tag(name = "Discover", description = "Public cached discovery feed endpoints.")
public class DiscoverController {

    private final DiscoverService discoverService;

    @GetMapping("/trending-movies")
    @Operation(summary = "Get trending movies", description = "Returns locally cached trending movies.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Trending movies returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = DiscoveryMediaItemDTO.class)))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<DiscoveryMediaItemDTO>> getTrendingMovies() {
        return ResponseEntity.ok(discoverService.getTrendingMovies());
    }

    @GetMapping("/trending-shows")
    @Operation(summary = "Get trending shows", description = "Returns locally cached trending shows.")
    public ResponseEntity<List<DiscoveryMediaItemDTO>> getTrendingShows() {
        return ResponseEntity.ok(discoverService.getTrendingShows());
    }

    @GetMapping("/popular-now")
    @Operation(summary = "Get popular now", description = "Returns the locally cached popular-now feed.")
    public ResponseEntity<List<DiscoveryMediaItemDTO>> getPopularNow() {
        return ResponseEntity.ok(discoverService.getPopularNow());
    }

    @GetMapping("/airing-today")
    @Operation(summary = "Get airing today", description = "Returns locally cached shows airing today.")
    public ResponseEntity<List<DiscoveryMediaItemDTO>> getAiringToday() {
        return ResponseEntity.ok(discoverService.getAiringToday());
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming releases", description = "Returns the locally cached upcoming feed.")
    public ResponseEntity<List<DiscoveryMediaItemDTO>> getUpcoming() {
        return ResponseEntity.ok(discoverService.getUpcoming());
    }

    @GetMapping("/recommended-later")
    @Operation(summary = "Get recommended later", description = "Returns the locally cached placeholder recommendation feed.")
    public ResponseEntity<List<DiscoveryMediaItemDTO>> getRecommendedLater() {
        return ResponseEntity.ok(discoverService.getRecommendedLater());
    }
}
