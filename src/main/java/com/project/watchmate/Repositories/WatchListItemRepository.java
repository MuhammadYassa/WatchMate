package com.project.watchmate.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.watchmate.Models.WatchListItem;

public interface WatchListItemRepository extends JpaRepository<WatchListItem, Long>{

}
