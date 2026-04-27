package com.project.watchmate.Services;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Repositories.MediaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaResolutionService {

    private final MediaRepository mediaRepository;

    private final TmdbService tmdbService;

    @Transactional
    public Media resolveMediaByTmdbId(Long tmdbId, String typeStr) {
        return resolveMediaByTmdbId(tmdbId, parseMediaType(typeStr));
    }

    @Transactional
    public Media resolveMediaByTmdbId(Long tmdbId, MediaType type) {
        Long resolvedTmdbId = Objects.requireNonNull(tmdbId, "tmdbId");

        Media media = mediaRepository.findByTmdbId(resolvedTmdbId).orElse(null);
        if (media != null) {
            if (type != null && media.getType() != null && media.getType() != type) {
                throw new IllegalArgumentException("Media type does not match the requested media item.");
            }
            return media;
        }

        if (type == null) {
            throw new IllegalArgumentException("Media type is required when the media item has not been imported yet.");
        }

        Media importedMedia = tmdbService.fetchMediaByTmdbId(resolvedTmdbId, type);
        if (importedMedia == null) {
            throw new MediaNotFoundException("TMDB media not found for ID: " + resolvedTmdbId);
        }

        return mediaRepository.save(importedMedia);
    }

    public MediaType parseMediaType(String typeStr) {
        if (typeStr == null || typeStr.isBlank()) {
            return null;
        }

        try {
            return MediaType.valueOf(typeStr.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid media type. Allowed values: MOVIE, SHOW");
        }
    }
}
