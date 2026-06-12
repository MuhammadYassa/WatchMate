package com.project.watchmate.show.tracking.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.show.tracking.domain.UserShowTracking;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;

public interface UserShowTrackingRepository extends JpaRepository<UserShowTracking, Long> {

    Optional<UserShowTracking> findByUserAndMedia(Users user, Media media);

    @Query("""
        select ust
        from UserShowTracking ust
        join fetch ust.media media
        where ust.user = :user
        and media.id in :mediaIds
        """)
    List<UserShowTracking> findByUserAndMediaIdIn(@Param("user") Users user, @Param("mediaIds") Collection<Long> mediaIds);

    @Query("select distinct ust from UserShowTracking ust " +
        "left join fetch ust.episodeWatches ew " +
        "where ust.user = :user and ust.media = :media")
    Optional<UserShowTracking> findWithEpisodeWatchesByUserAndMedia(@Param("user") Users user, @Param("media") Media media);

    @Query("select count(ust) " +
        "from UserShowTracking ust join ust.media m " +
        "where ust.user = :user " +
        "and ust.status = com.project.watchmate.media.catalog.domain.WatchStatus.WATCHED " +
        "and m.type = com.project.watchmate.media.catalog.domain.MediaType.SHOW")
    long countWatchedShowsByUser(@Param("user") Users user);

    @Query(
        value = "select m " +
            "from UserShowTracking ust join ust.media m " +
            "where ust.user = :user " +
            "and ust.status = com.project.watchmate.media.catalog.domain.WatchStatus.WATCHED " +
            "and m.type = com.project.watchmate.media.catalog.domain.MediaType.SHOW " +
            "order by m.releaseDate desc, m.title desc, m.id desc",
        countQuery = "select count(m) " +
            "from UserShowTracking ust join ust.media m " +
            "where ust.user = :user " +
            "and ust.status = com.project.watchmate.media.catalog.domain.WatchStatus.WATCHED " +
            "and m.type = com.project.watchmate.media.catalog.domain.MediaType.SHOW"
    )
    Page<Media> findWatchedShowsByUser(@Param("user") Users user, Pageable pageable);

    @Query("select ust " +
        "from UserShowTracking ust " +
        "join fetch ust.media m " +
        "where ust.user = :user " +
        "and ust.status in :statuses " +
        "order by coalesce(ust.lastWatchedAt, ust.updatedAt) desc, ust.updatedAt desc, ust.id desc")
    List<UserShowTracking> findContinueWatchingByUser(
        @Param("user") Users user,
        @Param("statuses") Collection<WatchStatus> statuses,
        Pageable pageable
    );

    @Query("select ust " +
        "from UserShowTracking ust " +
        "join fetch ust.media m " +
        "where ust.user = :user " +
        "and ust.status in :statuses " +
        "and m.nextEpisodeAirDate is not null " +
        "and m.nextEpisodeAirDate >= :today " +
        "order by m.nextEpisodeAirDate asc, m.title asc, ust.id asc")
    List<UserShowTracking> findUpcomingEpisodesByUser(
        @Param("user") Users user,
        @Param("statuses") Collection<WatchStatus> statuses,
        @Param("today") LocalDate today
    );

    @Query("select ust " +
        "from UserShowTracking ust " +
        "join fetch ust.media m " +
        "where ust.user = :user " +
        "and ust.status in :statuses " +
        "and m.nextEpisodeAirDate between :from and :to " +
        "order by m.nextEpisodeAirDate asc, m.title asc, ust.id asc")
    List<UserShowTracking> findCalendarItemsByUser(
        @Param("user") Users user,
        @Param("statuses") Collection<WatchStatus> statuses,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );
}




