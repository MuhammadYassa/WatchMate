package com.project.watchmate.Controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Models.Users;
import com.project.watchmate.Services.FavouriteService;
import com.project.watchmate.Dto.FavouriteStatusDTO;
import com.project.watchmate.Dto.UserFavouritesDTO;
import com.project.watchmate.Models.UserPrincipal;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.validation.constraints.Min;



@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/favourites")
@Validated
public class FavouriteController {

    private final FavouriteService favouriteService;

    @PostMapping("/add/{tmdbId}")
    public ResponseEntity<FavouriteStatusDTO> addFavourite(@PathVariable @Min(1) Long tmdbId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        FavouriteStatusDTO response = favouriteService.addToFavourites(tmdbId, user);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/remove/{tmdbId}")
    public ResponseEntity<FavouriteStatusDTO> removeFavourite(@PathVariable @Min(1) Long tmdbId, @AuthenticationPrincipal UserPrincipal userPrincipal){
        Users user = userPrincipal.getUser();
        FavouriteStatusDTO response = favouriteService.removeFromFavourites(tmdbId, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<UserFavouritesDTO> allFavourites(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        UserFavouritesDTO response = favouriteService.getUserFavourites(user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check/{tmdbId}")
    public ResponseEntity<FavouriteStatusDTO> isFavourited(@PathVariable @Min(1) Long tmdbId, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        FavouriteStatusDTO response = favouriteService.isFavourited(tmdbId, user);
        return ResponseEntity.ok(response);
    }
    
    
}
