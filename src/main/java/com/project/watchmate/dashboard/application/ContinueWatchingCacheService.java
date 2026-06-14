package com.project.watchmate.dashboard.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.common.cache.WatchMateCacheNames;
import com.project.watchmate.dashboard.dto.ContinueWatchingItemDTO;
import com.project.watchmate.dashboard.mapper.DashboardMapper;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.movie.tracking.domain.UserMediaStatus;
import com.project.watchmate.movie.tracking.persistence.UserMediaStatusRepository;
import com.project.watchmate.show.tracking.domain.UserShowTracking;
import com.project.watchmate.show.tracking.persistence.UserShowTrackingRepository;
import com.project.watchmate.user.domain.Users;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContinueWatchingCacheService {

    private static final List<WatchStatus> CONTINUE_WATCHING_STATUSES = List.of(WatchStatus.WATCHING);

    private final UserMediaStatusRepository userMediaStatusRepository;

    private final UserShowTrackingRepository userShowTrackingRepository;

    private final DashboardMapper dashboardMapper;

    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames = WatchMateCacheNames.CONTINUE_WATCHING,
        key = "T(com.project.watchmate.common.cache.WatchMateCacheKeys).user(#user.id)",
        unless = "#result == null"
    )
    public List<ContinueWatchingItemDTO> getContinueWatchingItems(Users user, int maxLimit) {
        List<UserMediaStatus> movieStatuses = userMediaStatusRepository.findContinueWatchingMoviesByUser(
            user,
            CONTINUE_WATCHING_STATUSES,
            PageRequest.of(0, maxLimit)
        );
        List<UserShowTracking> trackings = userShowTrackingRepository.findContinueWatchingByUser(
            user,
            CONTINUE_WATCHING_STATUSES,
            PageRequest.of(0, maxLimit)
        );

        List<ContinueWatchingItemDTO> items = new ArrayList<>();
        items.addAll(movieStatuses.stream().map(dashboardMapper::mapToContinueWatchingItem).toList());
        items.addAll(trackings.stream().map(dashboardMapper::mapToContinueWatchingItem).toList());
        items.sort((left, right) -> {
            LocalDateTime leftSort = left.getLastWatchedAt() != null ? left.getLastWatchedAt() : left.getUpdatedAt();
            LocalDateTime rightSort = right.getLastWatchedAt() != null ? right.getLastWatchedAt() : right.getUpdatedAt();
            int byTimestamp = Comparator.nullsLast(LocalDateTime::compareTo).compare(rightSort, leftSort);
            if (byTimestamp != 0) {
                return byTimestamp;
            }
            return Comparator.nullsLast(Long::compareTo).compare(right.getTmdbId(), left.getTmdbId());
        });
        return items;
    }
}
