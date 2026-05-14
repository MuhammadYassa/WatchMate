package com.project.watchmate.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.watchmate.Models.ShowSeason;

public interface ShowSeasonRepository extends JpaRepository<ShowSeason, Long> {

    Optional<ShowSeason> findByMediaIdAndSeasonNumber(Long mediaId, Integer seasonNumber);

    List<ShowSeason> findAllByMediaIdOrderBySeasonNumberAsc(Long mediaId);
}
