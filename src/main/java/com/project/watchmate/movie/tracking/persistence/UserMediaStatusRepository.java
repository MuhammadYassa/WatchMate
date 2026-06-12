package com.project.watchmate.movie.tracking.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.movie.tracking.domain.UserMediaStatus;
import com.project.watchmate.user.domain.Users;

public interface UserMediaStatusRepository extends JpaRepository<UserMediaStatus, Long>{

    Optional<UserMediaStatus> findByUserAndMedia(Users user, Media media);

    @Query("""
        select ums
        from UserMediaStatus ums
        join fetch ums.media media
        where ums.user = :user
        and media.id in :mediaIds
        """)
    List<UserMediaStatus> findByUserAndMediaIdIn(@Param("user") Users user, @Param("mediaIds") Collection<Long> mediaIds);

    @Query("select count(ums) " +
        "from UserMediaStatus ums join ums.media m " +
        "where ums.user = :user " +
        "and ums.status = com.project.watchmate.media.catalog.domain.WatchStatus.WATCHED " +
        "and m.type = com.project.watchmate.media.catalog.domain.MediaType.MOVIE")
    long countWatchedMoviesByUser(@Param("user") Users user);

    @Query(
        value = "select m " +
            "from UserMediaStatus ums join ums.media m " +
            "where ums.user = :user " +
            "and ums.status = com.project.watchmate.media.catalog.domain.WatchStatus.WATCHED " +
            "and m.type = com.project.watchmate.media.catalog.domain.MediaType.MOVIE " +
            "order by m.releaseDate desc, m.title desc, m.id desc",
        countQuery = "select count(m) " +
            "from UserMediaStatus ums join ums.media m " +
            "where ums.user = :user " +
            "and ums.status = com.project.watchmate.media.catalog.domain.WatchStatus.WATCHED " +
            "and m.type = com.project.watchmate.media.catalog.domain.MediaType.MOVIE"
    )
    Page<Media> findWatchedMoviesByUser(@Param("user") Users user, Pageable pageable);

    @Query("select ums " +
        "from UserMediaStatus ums " +
        "join fetch ums.media m " +
        "where ums.user = :user " +
        "and ums.status in :statuses " +
        "and m.type = com.project.watchmate.media.catalog.domain.MediaType.MOVIE " +
        "order by ums.updatedAt desc, ums.id desc")
    List<UserMediaStatus> findContinueWatchingMoviesByUser(
        @Param("user") Users user,
        @Param("statuses") Collection<WatchStatus> statuses,
        Pageable pageable
    );

}





