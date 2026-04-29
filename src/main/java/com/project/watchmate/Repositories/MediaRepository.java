package com.project.watchmate.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;

import java.util.List;
import java.util.Optional;


@Repository
public interface MediaRepository extends JpaRepository<Media, Long>{

    List<Media> findAllByTmdbId(Long tmdbId);

    Optional<Media> findByTmdbIdAndType(Long tmdbId, MediaType type);

    boolean existsByTmdbIdAndType(Long tmdbId, MediaType type);

}
