package com.project.watchmate.Controllers;

import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Dto.CreateWatchListDTO;
import com.project.watchmate.Dto.RenameWatchListDTO;
import com.project.watchmate.Dto.WatchListDTO;
import com.project.watchmate.Models.UserPrincipal;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Services.WatchListService;

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
public class WatchListController {

    private final WatchListService watchListService;

    @GetMapping()
    public ResponseEntity<List<WatchListDTO>> getAllWatchLists(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        List<WatchListDTO> allWatchLists = watchListService.getAllWatchLists(user);
        return ResponseEntity.ok(allWatchLists);
    }

    @PostMapping("")
    public ResponseEntity<WatchListDTO> createWatchList(@AuthenticationPrincipal UserPrincipal userPrincipal, @Valid @RequestBody CreateWatchListDTO dto) {
        Users user = userPrincipal.getUser();
        WatchListDTO created = watchListService.createWatchList(user, dto.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWatchList(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable @Min(1) Long id){
        Users user = userPrincipal.getUser();
        watchListService.deleteWatchList(user, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WatchListDTO> renameWatchList(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable @Min(1) Long id, @Valid @RequestBody RenameWatchListDTO dto) {
        Users user = userPrincipal.getUser();
        WatchListDTO renamed = watchListService.renameWatchList(user, id, dto.getNewName());
        return ResponseEntity.ok(renamed);
    }

    @PostMapping("/{watchListId}/items/{tmdbId}")
    public ResponseEntity<WatchListDTO> addMediaToWatchList(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable @Min(1) Long watchListId, @PathVariable @Min(1) Long tmdbId) {
        Users user = userPrincipal.getUser();
        WatchListDTO dto = watchListService.addMediaToWatchList(user, watchListId, tmdbId);
        return ResponseEntity.ok(dto);
    }
    
    @DeleteMapping("/{watchListId}/items/{tmdbId}")
    public ResponseEntity<WatchListDTO> removeMediaFromWatchList(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable @Min(1) Long watchListId, @PathVariable @Min(1) Long tmdbId) {
        Users user = userPrincipal.getUser();
        WatchListDTO dto = watchListService.removeMediaFromWatchList(user, watchListId, tmdbId);
        return ResponseEntity.ok(dto);
    }
    
}
