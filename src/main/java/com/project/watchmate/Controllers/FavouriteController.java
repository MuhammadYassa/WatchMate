package com.project.watchmate.Controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Dto.ApiError;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Services.FavouriteService;
import com.project.watchmate.Dto.FavouriteStatusDTO;
import com.project.watchmate.Dto.UserFavouritesDTO;
import com.project.watchmate.Models.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.constraints.Min;



@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/favourites")
@Validated
@Tag(name = "Favourites", description = "Authenticated favourites management endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class FavouriteController {

    private final FavouriteService favouriteService;

    @PostMapping("/add/{tmdbId}")
    @Operation(summary = "Add favourite", description = "Marks a media item as a favourite for the authenticated user. Provide type when the item may need to be imported first.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Favourite added", content = @Content(schema = @Schema(implementation = FavouriteStatusDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Media not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Media already favourited", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FavouriteStatusDTO> addFavourite(@PathVariable @Min(1) Long tmdbId, @RequestParam(value = "type", required = false) String type, @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        FavouriteStatusDTO response = favouriteService.addToFavourites(tmdbId, type, user);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/remove/{tmdbId}")
    @Operation(summary = "Remove favourite", description = "Removes a media item from the authenticated user's favourites. Provide type when the item may need to be imported first.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Favourite removed", content = @Content(schema = @Schema(implementation = FavouriteStatusDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Media not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FavouriteStatusDTO> removeFavourite(@PathVariable @Min(1) Long tmdbId, @RequestParam(value = "type", required = false) String type, @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal){
        Users user = userPrincipal.getUser();
        FavouriteStatusDTO response = favouriteService.removeFromFavourites(tmdbId, type, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    @Operation(summary = "List favourites", description = "Returns all favourites for the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Favourites returned", content = @Content(schema = @Schema(implementation = UserFavouritesDTO.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<UserFavouritesDTO> allFavourites(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        UserFavouritesDTO response = favouriteService.getUserFavourites(user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check/{tmdbId}")
    @Operation(summary = "Check favourite status", description = "Returns whether a media item is favourited by the authenticated user. Provide type when the item may need to be imported first.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Favourite status returned", content = @Content(schema = @Schema(implementation = FavouriteStatusDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path parameter", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Media not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FavouriteStatusDTO> isFavourited(@PathVariable @Min(1) Long tmdbId, @RequestParam(value = "type", required = false) String type, @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        FavouriteStatusDTO response = favouriteService.isFavourited(tmdbId, type, user);
        return ResponseEntity.ok(response);
    }
    
    
}
