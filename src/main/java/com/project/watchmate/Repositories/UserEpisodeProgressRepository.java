package com.project.watchmate.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.watchmate.Models.UserEpisodeProgress;
import com.project.watchmate.Models.UserShowProgress;

public interface UserEpisodeProgressRepository extends JpaRepository<UserEpisodeProgress, Long> {

    List<UserEpisodeProgress> findByUserShowProgressOrderBySeasonNumberAscEpisodeNumberAsc(UserShowProgress userShowProgress);

    List<UserEpisodeProgress> findByUserShowProgressAndWatchedTrueOrderBySeasonNumberAscEpisodeNumberAsc(UserShowProgress userShowProgress);

    Optional<UserEpisodeProgress> findByUserShowProgressAndSeasonNumberAndEpisodeNumber(
        UserShowProgress userShowProgress,
        Integer seasonNumber,
        Integer episodeNumber
    );
}
