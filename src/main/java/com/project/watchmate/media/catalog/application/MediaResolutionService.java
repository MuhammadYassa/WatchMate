package com.project.watchmate.media.catalog.application;

import com.project.watchmate.media.tmdb.application.TmdbService;

import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.project.watchmate.common.cache.WatchMateCacheEvictionService;
import com.project.watchmate.common.error.MediaNotFoundException;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.catalog.persistence.MediaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaResolutionService {

    private final MediaRepository mediaRepository;

    private final TmdbService tmdbService;

    private final WatchMateCacheEvictionService cacheEvictionService;

    private final PlatformTransactionManager transactionManager;

    public Media resolveMediaByTmdbId(Long tmdbId, String typeStr) {
        return resolveMediaByTmdbId(tmdbId, parseMediaType(typeStr));
    }

    public Media resolveMediaByTmdbId(Long tmdbId, MediaType type) {
        Long resolvedTmdbId = Objects.requireNonNull(tmdbId, "tmdbId");

        Media media;
        try {
            media = executeRequiresNew(() -> resolveMediaByTmdbIdInTransaction(resolvedTmdbId, type));
        } catch (DataIntegrityViolationException ex) {
            media = recoverDuplicateMedia(resolvedTmdbId, type, ex);
        }
        return attachToCurrentTransaction(media);
    }

    private Media resolveMediaByTmdbIdInTransaction(Long resolvedTmdbId, MediaType type) {
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

        Media saved = mediaRepository.saveAndFlush(importedMedia);
        cacheEvictionService.evictPublicMediaDetailBase(saved.getType(), saved.getTmdbId());
        return saved;
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

    private Media attachToCurrentTransaction(Media media) {
        if (media == null || media.getId() == null) {
            return media;
        }
        return mediaRepository.findById(media.getId()).orElse(media);
    }

    private Media recoverDuplicateMedia(Long tmdbId, MediaType type, DataIntegrityViolationException ex) {
        if (type == null || !isMediaTmdbTypeDuplicate(ex)) {
            throw ex;
        }

        return executeRequiresNew(() -> mediaRepository.findByTmdbIdAndType(tmdbId, type)
            .orElseThrow(() -> ex));
    }

    private <T> T executeRequiresNew(java.util.function.Supplier<T> action) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(status -> action.get());
    }

    private boolean isMediaTmdbTypeDuplicate(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() == null ? ex.getMessage() : ex.getMostSpecificCause().getMessage();
        return message != null && message.contains("uq_media_tmdb_id_type");
    }
}




