package com.project.watchmate.Repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.UserShowProgress;
import com.project.watchmate.Models.Users;

public interface UserShowProgressRepository extends JpaRepository<UserShowProgress, Long> {

    Optional<UserShowProgress> findByUserAndMedia(Users user, Media media);

    @Query("select distinct usp from UserShowProgress usp " +
        "left join fetch usp.episodeProgress ep " +
        "where usp.user = :user and usp.media = :media")
    Optional<UserShowProgress> findWithEpisodeProgressByUserAndMedia(@Param("user") Users user, @Param("media") Media media);
}
