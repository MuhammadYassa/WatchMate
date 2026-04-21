package com.project.watchmate.Controllers;

import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Dto.ApiError;
import com.project.watchmate.Dto.CreateWatchListDTO;
import com.project.watchmate.Dto.RenameWatchListDTO;
import com.project.watchmate.Dto.WatchListDTO;
import com.project.watchmate.Models.UserPrincipal;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Services.WatchListService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/watchlists")
@RequiredArgsConstructor
@Validated
@Tag(name = "Watchlists", description = "Authenticated watchlist management endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class WatchListController {

    private final WatchListService watchListService;

    @GetMapping()
    @Operation(summary = "List user watchlists", description = "Returns all watchlists owned by the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Watchlists returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = WatchListDTO.class)))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<WatchListDTO>> getAllWatchLists(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        List<WatchListDTO> allWatchLists = watchListService.getAllWatchLists(user);
        return ResponseEntity.ok(allWatchLists);
    }

    @PostMapping("")
    @Operation(summary = "Create watchlist", description = "Creates a new watchlist owned by the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Watchlist created", content = @Content(schema = @Schema(implementation = WatchListDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Watchlist already exists", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<WatchListDTO> createWatchList(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal, @Valid @RequestBody CreateWatchListDTO dto) {
        Users user = userPrincipal.getUser();
        WatchListDTO created = watchListService.createWatchList(user, dto.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete watchlist", description = "Deletes a watchlist owned by the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Watchlist deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Watchlist not owned by user", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Watchlist not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> deleteWatchList(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable @Min(1) Long id){
        Users user = userPrincipal.getUser();
        watchListService.deleteWatchList(user, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Rename watchlist", description = "Renames a watchlist owned by the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Watchlist renamed", content = @Content(schema = @Schema(implementation = WatchListDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Watchlist not owned by user", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Watchlist not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Watchlist name conflict", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<WatchListDTO> renameWatchList(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable @Min(1) Long id, @Valid @RequestBody RenameWatchListDTO dto) {
        Users user = userPrincipal.getUser();
        WatchListDTO renamed = watchListService.renameWatchList(user, id, dto.getNewName());
        return ResponseEntity.ok(renamed);
    }

    @PostMapping("/{watchListId}/items/{tmdbId}")
    @Operation(summary = "Add watchlist item", description = "Adds an existing media record to a watchlist owned by the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Media added to watchlist", content = @Content(schema = @Schema(implementation = WatchListDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Watchlist not owned by user", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Watchlist or media not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Media already in watchlist", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<WatchListDTO> addMediaToWatchList(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable @Min(1) Long watchListId, @PathVariable @Min(1) Long tmdbId) {
        Users user = userPrincipal.getUser();
        WatchListDTO dto = watchListService.addMediaToWatchList(user, watchListId, tmdbId);
        return ResponseEntity.ok(dto);
    }
    
    @DeleteMapping("/{watchListId}/items/{tmdbId}")
    @Operation(summary = "Remove watchlist item", description = "Removes a media record from a watchlist owned by the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Media removed from watchlist", content = @Content(schema = @Schema(implementation = WatchListDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Watchlist not owned by user", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Watchlist, media, or watchlist item not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<WatchListDTO> removeMediaFromWatchList(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable @Min(1) Long watchListId, @PathVariable @Min(1) Long tmdbId) {
        Users user = userPrincipal.getUser();
        WatchListDTO dto = watchListService.removeMediaFromWatchList(user, watchListId, tmdbId);
        return ResponseEntity.ok(dto);
    }
    
}