package com.project.watchmate.Repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.Users;

public interface UserMediaStatusRepository extends JpaRepository<UserMediaStatus, Long>{

    Optional<UserMediaStatus> findByUserAndMedia(Users user, Media media);

    @Query("select count(ums) " +
        "from UserMediaStatus ums join ums.media m " +
        "where ums.user = :user " +
        "and ums.status = com.project.watchmate.Models.WatchStatus.WATCHED " +
        "and m.type = com.project.watchmate.Models.MediaType.MOVIE")
    long countWatchedMoviesByUser(@Param("user") Users user);

    @Query("select count(ums) " +
        "from UserMediaStatus ums join ums.media m " +
        "where ums.user = :user " +
        "and ums.status = com.project.watchmate.Models.WatchStatus.WATCHED " +
        "and m.type = com.project.watchmate.Models.MediaType.SHOW")
    long countWatchedShowsByUser(@Param("user") Users user);

    @Query("select m " +
        "from UserMediaStatus ums join ums.media m " +
        "where ums.user = :user " +
        "and ums.status = com.project.watchmate.Models.WatchStatus.WATCHED " +
        "and m.type = com.project.watchmate.Models.MediaType.MOVIE")
    Page<Media> findWatchedMoviesByUser(@Param("user") Users user, Pageable pageable);

    @Query("select m " +
        "from UserMediaStatus ums join ums.media m " +
        "where ums.user = :user " +
        "and ums.status = com.project.watchmate.Models.WatchStatus.WATCHED " +
        "and m.type = com.project.watchmate.Models.MediaType.SHOW")
    Page<Media> findWatchedShowsByUser(@Param("user") Users user, Pageable pageable);

}
