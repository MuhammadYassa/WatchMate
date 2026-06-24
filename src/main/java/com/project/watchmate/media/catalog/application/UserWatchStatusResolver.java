package com.project.watchmate.media.catalog.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.movie.tracking.persistence.UserMediaStatusRepository;
import com.project.watchmate.show.tracking.persistence.UserShowTrackingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserWatchStatusResolver {

    private final UserMediaStatusRepository userMediaStatusRepository;

    private final UserShowTrackingRepository userShowTrackingRepository;

    @Transactional(readOnly = true)
    public WatchStatus resolveWatchStatus(Users user, Media media) {
        if (user == null || media == null) {
            return WatchStatus.NONE;
        }

        if (media.getType() == MediaType.SHOW) {
            return userShowTrackingRepository.findByUserAndMedia(user, media)
                .map(tracking -> tracking.getStatus())
                .orElse(WatchStatus.NONE);
        }

        return userMediaStatusRepository.findByUserAndMedia(user, media)
            .map(status -> status.getStatus())
            .orElse(WatchStatus.NONE);
    }

    /**
     * Batch-resolves watch statuses for a list of media items in 2 queries
     * (one for movies, one for shows) instead of N queries.
     *
     * @return map of media ID → WatchStatus; items with no tracking entry are absent
     *         (callers should default to {@link WatchStatus#NONE})
     */
    @Transactional(readOnly = true)
    public Map<Long, WatchStatus> resolveWatchStatusBatch(Users user, List<Media> mediaItems) {
        if (user == null || mediaItems == null || mediaItems.isEmpty()) {
            return Map.of();
        }

        Map<Long, WatchStatus> result = new HashMap<>();

        List<Long> movieIds = mediaItems.stream()
            .filter(m -> m.getType() == MediaType.MOVIE)
            .map(Media::getId)
            .collect(Collectors.toList());

        List<Long> showIds = mediaItems.stream()
            .filter(m -> m.getType() == MediaType.SHOW)
            .map(Media::getId)
            .collect(Collectors.toList());

        if (!movieIds.isEmpty()) {
            userMediaStatusRepository.findByUserAndMediaIdIn(user, movieIds)
                .forEach(ums -> result.put(ums.getMedia().getId(), ums.getStatus()));
        }

        if (!showIds.isEmpty()) {
            userShowTrackingRepository.findByUserAndMediaIdIn(user, showIds)
                .forEach(ust -> result.put(ust.getMedia().getId(), ust.getStatus()));
        }

        return result;
    }
}





