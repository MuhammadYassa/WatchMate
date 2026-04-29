package com.project.watchmate.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Dto.ApiError;
import com.project.watchmate.Dto.HomeResponseDTO;
import com.project.watchmate.Dto.HomeStatusDTO;
import com.project.watchmate.Services.HomeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/home")
@Tag(name = "Home", description = "Public homepage discovery endpoints backed by the local cache.")
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    @Operation(summary = "Get homepage discovery payload", description = "Returns all homepage buckets from locally cached data only.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Homepage data returned", content = @Content(schema = @Schema(implementation = HomeResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<HomeResponseDTO> getHome() {
        return ResponseEntity.ok(homeService.getHome());
    }

    @GetMapping("/status")
    @Operation(summary = "Get homepage cache status", description = "Returns the last discovery sync status and bucket counts.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Homepage status returned", content = @Content(schema = @Schema(implementation = HomeStatusDTO.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<HomeStatusDTO> getHomeStatus() {
        return ResponseEntity.ok(homeService.getHomeStatus());
    }
}
