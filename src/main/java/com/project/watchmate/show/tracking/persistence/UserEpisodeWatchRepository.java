package com.project.watchmate.show.tracking.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.watchmate.show.tracking.domain.UserEpisodeWatch;
import com.project.watchmate.show.tracking.domain.UserShowTracking;

public interface UserEpisodeWatchRepository extends JpaRepository<UserEpisodeWatch, Long> {

    List<UserEpisodeWatch> findByUserShowTrackingOrderBySeasonNumberAscEpisodeNumberAsc(UserShowTracking userShowTracking);

    Optional<UserEpisodeWatch> findByUserShowTrackingAndSeasonNumberAndEpisodeNumber(
        UserShowTracking userShowTracking,
        Integer seasonNumber,
        Integer episodeNumber
    );
}


