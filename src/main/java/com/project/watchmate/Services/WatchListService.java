package com.project.watchmate.Services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.watchmate.Dto.WatchListDTO;
import com.project.watchmate.Exception.UnauthorizedWatchListAccessException;
import com.project.watchmate.Exception.WatchListNotFoundException;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchList;
import com.project.watchmate.Repositories.WatchListRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WatchListService {

    private final WatchListRepository watchListRepository;
    
    public WatchList createWatchList(Users user, String name) {
        if (watchListRepository.existsByUserAndNameIgnoreCase(user, name)){
            throw new IllegalArgumentException("Watchlist Already Exists.");
        }
        WatchList watchList = WatchList.builder()
        .name(name)
        .user(user)
        .build();

        return watchListRepository.save(watchList);
    }

    public void deleteWatchList(Users user, Long id) {
        WatchList watchList = watchListRepository.findById(id).orElseThrow(() -> new WatchListNotFoundException("WatchList not Found"));

        if (!watchList.getUser().getId().equals(user.getId())){
            throw new UnauthorizedWatchListAccessException("You do not own this watchlist");
        }

        watchListRepository.delete(watchList);
    }

    public WatchList renameWatchList(Users user, Long id, String newName) {
        WatchList watchList = watchListRepository.findById(id).orElseThrow(() -> new WatchListNotFoundException("WatchList not Found."));

        if (!watchList.getUser().getId().equals(user.getId())){
            throw new UnauthorizedWatchListAccessException("You do not own this WatchList.");
        }

        if (watchListRepository.existsByUserAndNameIgnoreCase(user, newName)){
            throw new IllegalArgumentException("A WatchList with this name Already Exists");
        }

        watchList.setName(newName);
        return watchListRepository.save(watchList);
    }

    public List<WatchListDTO> getAllWatchLists(Users user) {
        throw new RuntimeException("test");
    }

}
