package com.project.watchmate.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.watchmate.Models.ShowEpisode;

public interface ShowEpisodeRepository extends JpaRepository<ShowEpisode, Long> {

    List<ShowEpisode> findAllByMediaIdOrderBySeasonNumberAscEpisodeNumberAsc(Long mediaId);

    List<ShowEpisode> findAllByMediaIdAndSeasonNumberOrderByEpisodeNumberAsc(Long mediaId, Integer seasonNumber);

    Optional<ShowEpisode> findByMediaIdAndSeasonNumberAndEpisodeNumber(Long mediaId, Integer seasonNumber, Integer episodeNumber);

    long countByMediaIdAndSeasonNumber(Long mediaId, Integer seasonNumber);

    void deleteByMediaIdAndSeasonNumber(Long mediaId, Integer seasonNumber);
}
