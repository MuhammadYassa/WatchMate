package com.project.watchmate.dashboard.application;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.dashboard.dto.CalendarResponseDTO;
import com.project.watchmate.dashboard.dto.ContinueWatchingResponseDTO;
import com.project.watchmate.dashboard.dto.UpcomingEpisodesResponseDTO;
import com.project.watchmate.dashboard.mapper.DashboardMapper;
import com.project.watchmate.show.tracking.domain.UserShowTracking;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.show.tracking.persistence.UserShowTrackingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int DEFAULT_LIMIT = 20;

    private static final int MAX_LIMIT = 50;

    private static final List<WatchStatus> UPCOMING_EPISODE_TRACKING_STATUSES = List.of(
        WatchStatus.WATCHING,
        WatchStatus.UP_TO_DATE,
        WatchStatus.TO_WATCH
    );

    private final UserShowTrackingRepository userShowTrackingRepository;

    private final DashboardMapper dashboardMapper;

    private final ContinueWatchingCacheService continueWatchingCacheService;

    @Transactional(readOnly = true)
    public ContinueWatchingResponseDTO getContinueWatching(Users user, Integer limit) {
        int resolvedLimit = normalizeLimit(limit);

        return ContinueWatchingResponseDTO.builder()
            .items(continueWatchingCacheService.getContinueWatchingItems(user, MAX_LIMIT).stream()
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
        if (ChronoUnit.DAYS.between(from, to) > 90) {
            throw new IllegalArgumentException("Date range cannot exceed 90 days.");
        }
    }
}





