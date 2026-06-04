package com.project.watchmate.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.watchmate.Models.UserEpisodeWatch;
import com.project.watchmate.Models.UserShowTracking;

public interface UserEpisodeWatchRepository extends JpaRepository<UserEpisodeWatch, Long> {

    List<UserEpisodeWatch> findByUserShowTrackingOrderBySeasonNumberAscEpisodeNumberAsc(UserShowTracking userShowTracking);

    List<UserEpisodeWatch> findByUserShowTrackingOrderByWatchedAtAscSeasonNumberAscEpisodeNumberAsc(UserShowTracking userShowTracking);

    Optional<UserEpisodeWatch> findByUserShowTrackingAndSeasonNumberAndEpisodeNumber(
        UserShowTracking userShowTracking,
        Integer seasonNumber,
        Integer episodeNumber
    );
}
