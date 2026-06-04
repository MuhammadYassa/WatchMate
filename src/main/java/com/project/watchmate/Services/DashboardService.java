package com.project.watchmate.Services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.Dto.CalendarResponseDTO;
import com.project.watchmate.Dto.ContinueWatchingResponseDTO;
import com.project.watchmate.Dto.UpcomingEpisodesResponseDTO;
import com.project.watchmate.Dto.ContinueWatchingItemDTO;
import com.project.watchmate.Mappers.DashboardMapper;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.UserShowTracking;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.UserShowTrackingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int DEFAULT_LIMIT = 20;

    private static final int MAX_LIMIT = 50;

    private static final List<WatchStatus> CONTINUE_WATCHING_STATUSES = List.of(WatchStatus.WATCHING);

    private static final List<WatchStatus> UPCOMING_EPISODE_TRACKING_STATUSES = List.of(
        WatchStatus.WATCHING,
        WatchStatus.UP_TO_DATE,
        WatchStatus.TO_WATCH
    );

    private final UserMediaStatusRepository userMediaStatusRepository;

    private final UserShowTrackingRepository userShowTrackingRepository;

    private final DashboardMapper dashboardMapper;

    @Transactional(readOnly = true)
    public ContinueWatchingResponseDTO getContinueWatching(Users user, Integer limit) {
        int resolvedLimit = normalizeLimit(limit);
        List<UserMediaStatus> movieStatuses = userMediaStatusRepository.findContinueWatchingMoviesByUser(
            user,
            CONTINUE_WATCHING_STATUSES,
            PageRequest.of(0, resolvedLimit)
        );
        List<UserShowTracking> trackings = userShowTrackingRepository.findContinueWatchingByUser(
            user,
            CONTINUE_WATCHING_STATUSES,
            PageRequest.of(0, resolvedLimit)
        );

        List<ContinueWatchingItemDTO> items = new ArrayList<>();
        items.addAll(movieStatuses.stream().map(dashboardMapper::mapToContinueWatchingItem).toList());
        items.addAll(trackings.stream().map(dashboardMapper::mapToContinueWatchingItem).toList());
        items.sort((left, right) -> {
            java.time.LocalDateTime leftSort = left.getLastWatchedAt() != null ? left.getLastWatchedAt() : left.getUpdatedAt();
            java.time.LocalDateTime rightSort = right.getLastWatchedAt() != null ? right.getLastWatchedAt() : right.getUpdatedAt();
            int byTimestamp = java.util.Comparator.nullsLast(java.time.LocalDateTime::compareTo).compare(rightSort, leftSort);
            if (byTimestamp != 0) {
                return byTimestamp;
            }
            return java.util.Comparator.nullsLast(Long::compareTo).compare(right.getTmdbId(), left.getTmdbId());
        });

        return ContinueWatchingResponseDTO.builder()
            .items(items.stream()
                .limit(resolvedLimit)
                .toList())
            .build();
    }

    @Transactional(readOnly = true)
    public UpcomingEpisodesResponseDTO getUpcomingEpisodesForUser(Users user) {
        LocalDate today = LocalDate.now();
        List<UserShowTracking> trackings = userShowTrackingRepository.findUpcomingEpisodesByUser(
            user,
            UPCOMING_EPISODE_TRACKING_STATUSES,
            today
        );

        return UpcomingEpisodesResponseDTO.builder()
            .items(trackings.stream()
                .map(tracking -> dashboardMapper.mapToUpcomingEpisodeItem(tracking, today))
                .toList())
            .build();
    }

    @Transactional(readOnly = true)
    public CalendarResponseDTO getCalendarForUser(Users user, LocalDate from, LocalDate to) {
        validateDateRange(from, to);

        List<UserShowTracking> trackings = userShowTrackingRepository.findCalendarItemsByUser(
            user,
            UPCOMING_EPISODE_TRACKING_STATUSES,
            from,
            to
        );

        return CalendarResponseDTO.builder()
            .items(trackings.stream()
                .map(dashboardMapper::mapToCalendarItem)
                .toList())
            .build();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Parameter 'from' must be on or before 'to'.");
        }
    }
}
