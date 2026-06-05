package com.project.watchmate.watchlist.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.watchmate.watchlist.domain.WatchListItem;

public interface WatchListItemRepository extends JpaRepository<WatchListItem, Long>{

}


