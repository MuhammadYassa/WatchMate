package com.project.watchmate.Controllers;

import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Dto.CreateWatchListDTO;
import com.project.watchmate.Dto.RenameWatchListDTO;
import com.project.watchmate.Dto.WatchListDTO;
import com.project.watchmate.Models.UserPrincipal;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchList;
import com.project.watchmate.Services.WatchListService;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/watchlists")
@RequiredArgsConstructor
public class WatchListController {

    private final WatchListService watchListService;

    @GetMapping("/all")
    public ResponseEntity<List<WatchListDTO>> getAllWatchLists(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Users user = userPrincipal.getUser();
        List<WatchListDTO> allWatchLists = watchListService.getAllWatchLists(user);
        return ResponseEntity.ok(allWatchLists);
    }

    @PostMapping("/create")
    public ResponseEntity<WatchList> createWatchList(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody CreateWatchListDTO dto) {
        Users user = userPrincipal.getUser();
        WatchList created = watchListService.createWatchList(user, dto.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteWatchList(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable Long id){
        Users user = userPrincipal.getUser();
        watchListService.deleteWatchList(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rename/{id}")
    public ResponseEntity<WatchList> renameWatchList(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable Long id, @RequestBody RenameWatchListDTO dto) {
        Users user = userPrincipal.getUser();
        WatchList updated = watchListService.renameWatchList(user, id, dto.getNewName());
        return ResponseEntity.ok(updated);
    }
    
}
