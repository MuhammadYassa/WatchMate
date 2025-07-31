package com.project.watchmate.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchList;

public interface WatchListRepository extends JpaRepository<WatchList, Long>{

    Optional<WatchList> findByUserAndNameIgnoreCase(Users user, String name);

    List<WatchList> findAllByUser(Users user);

    boolean existsByUserAndNameIgnoreCase(Users user, String Name);
    
}
