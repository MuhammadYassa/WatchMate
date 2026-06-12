package com.project.watchmate.watchlist.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.watchmate.watchlist.domain.WatchListItem;

public interface WatchListItemRepository extends JpaRepository<WatchListItem, Long>{

    @Query("""
        select distinct item
        from WatchListItem item
        join fetch item.watchList watchList
        join fetch item.media media
        left join fetch media.genres
        where watchList.id in :watchListIds
        order by watchList.id asc, item.position asc, item.addedAt asc, item.id asc
        """)
    List<WatchListItem> findAllByWatchListIdInWithMediaAndGenres(@Param("watchListIds") Collection<Long> watchListIds);

}


