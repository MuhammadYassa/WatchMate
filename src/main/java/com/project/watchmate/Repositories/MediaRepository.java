package com.project.watchmate.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.watchmate.Models.Media;
import java.util.Optional;


@Repository
public interface MediaRepository extends JpaRepository<Media, Long>{

    Optional<Media> findByTmdbId(Long tmdbId);

}
