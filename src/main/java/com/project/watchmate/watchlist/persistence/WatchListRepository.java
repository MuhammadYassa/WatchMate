package com.project.watchmate.watchlist.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.project.watchmate.user.domain.Users;
import com.project.watchmate.watchlist.domain.WatchList;

public interface WatchListRepository extends JpaRepository<WatchList, Long>{

    Optional<WatchList> findByUserAndNameIgnoreCase(Users user, String name);

    List<WatchList> findAllByUser(Users user);

    boolean existsByUserAndNameIgnoreCase(Users user, String Name);

    Page<WatchList> findAllByUser(Users user, Pageable pageable);
    
}



