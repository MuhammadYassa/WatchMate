package com.project.watchmate.Services;

import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
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

        if (type != null) {
            Media media = mediaRepository.findByTmdbIdAndType(resolvedTmdbId, type).orElse(null);
            if (media != null) {
                return media;
            }
        } else {
            Media media = resolveWithoutType(resolvedTmdbId);
            if (media != null) {
                return media;
            }
        }

        if (type == null) {
            throw new IllegalArgumentException("Media type is required when the media item has not been imported yet.");
        }

        Media importedMedia = tmdbService.fetchMediaByTmdbId(resolvedTmdbId, type);
        if (importedMedia == null) {
            throw new MediaNotFoundException("TMDB media not found for ID: " + resolvedTmdbId);
        }

        try {
            return mediaRepository.save(importedMedia);
        } catch (DataIntegrityViolationException ex) {
            Media existingMedia = mediaRepository.findByTmdbIdAndType(resolvedTmdbId, type).orElseThrow(() -> ex);
            return existingMedia;
        }
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

    private Media resolveWithoutType(Long tmdbId) {
        List<Media> matchingMedia = mediaRepository.findAllByTmdbId(tmdbId);
        if (matchingMedia.isEmpty()) {
            return null;
        }
        if (matchingMedia.size() > 1) {
            throw new IllegalArgumentException("Multiple media items share this TMDB ID. Please supply the media type.");
        }
        return matchingMedia.get(0);
    }
}
