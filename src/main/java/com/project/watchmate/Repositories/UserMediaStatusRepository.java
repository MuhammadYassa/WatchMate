package com.project.watchmate.Repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.WatchStatus;
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

    @Query(
        value = "select m " +
            "from UserMediaStatus ums join ums.media m " +
            "where ums.user = :user " +
            "and ums.status = com.project.watchmate.Models.WatchStatus.WATCHED " +
            "and m.type = com.project.watchmate.Models.MediaType.MOVIE " +
            "order by m.releaseDate desc, m.title desc, m.id desc",
        countQuery = "select count(m) " +
            "from UserMediaStatus ums join ums.media m " +
            "where ums.user = :user " +
            "and ums.status = com.project.watchmate.Models.WatchStatus.WATCHED " +
            "and m.type = com.project.watchmate.Models.MediaType.MOVIE"
    )
    Page<Media> findWatchedMoviesByUser(@Param("user") Users user, Pageable pageable);

    @Query("select ums " +
        "from UserMediaStatus ums " +
        "join fetch ums.media m " +
        "where ums.user = :user " +
        "and ums.status in :statuses " +
        "and m.type = com.project.watchmate.Models.MediaType.MOVIE " +
        "order by ums.updatedAt desc, ums.id desc")
    List<UserMediaStatus> findContinueWatchingMoviesByUser(
        @Param("user") Users user,
        @Param("statuses") Collection<WatchStatus> statuses,
        Pageable pageable
    );

}
