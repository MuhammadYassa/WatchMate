package com.project.watchmate.Controllers;

import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Dto.MediaDetailsDTO;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.UserPrincipal;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Services.MediaService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @GetMapping("/{tmdbId}")
    public ResponseEntity<MediaDetailsDTO> getMediaDetails(@PathVariable Long tmdbId, @RequestParam ("type") String typeStr, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        MediaType type = MediaType.valueOf(typeStr.toUpperCase());
        Users user = userPrincipal.getUser();;
        MediaDetailsDTO dto = mediaService.getMediaDetails(tmdbId, type, user);
        return ResponseEntity.ok(dto);
    }
    
}
