package com.project.watchmate.Services;

import org.springframework.stereotype.Service;

import com.project.watchmate.Dto.MediaDetailsDTO;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.Users;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaService {
    public MediaDetailsDTO getMediaDetails(Long tmdbId, MediaType type, Users user){
        return new MediaDetailsDTO();
    }
}
