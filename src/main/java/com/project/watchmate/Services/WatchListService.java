package com.project.watchmate.Services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.Dto.MediaDetailsDTO;
import com.project.watchmate.Dto.WatchListDTO;
import com.project.watchmate.Exception.DuplicateWatchListMediaException;
import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Exception.MediaNotInWatchListException;
import com.project.watchmate.Exception.UnauthorizedWatchListAccessException;
import com.project.watchmate.Exception.WatchListNotFoundException;
import com.project.watchmate.Mappers.WatchMateMapper;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.Review;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchList;
import com.project.watchmate.Models.WatchListItem;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.ReviewRepository;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.WatchListRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WatchListService {

    private final WatchListRepository watchListRepository;

    private final MediaRepository mediaRepository;

    private final WatchMateMapper watchMateMapper;

    private final ReviewRepository reviewsRepo;

    private final UserMediaStatusRepository userMediaStatusRepository;
    
    public WatchListDTO createWatchList(Users user, String name) {
        if (watchListRepository.existsByUserAndNameIgnoreCase(user, name)){
            throw new IllegalArgumentException("Watchlist Already Exists.");
        }
        WatchList watchList = WatchList.builder()
        .name(name)
        .user(user)
        .build();

        watchListRepository.save(watchList);

        return mapToWatchListDTO(watchList);
    }

    public void deleteWatchList(Users user, Long id) {
        WatchList watchList = watchListRepository.findById(id).orElseThrow(() -> new WatchListNotFoundException("WatchList not Found"));

        if (!watchList.getUser().getId().equals(user.getId())){
            throw new UnauthorizedWatchListAccessException("You do not own this watchlist");
        }

        watchListRepository.delete(watchList);
    }

    public WatchListDTO renameWatchList(Users user, Long id, String newName) {
        WatchList watchList = watchListRepository.findById(id).orElseThrow(() -> new WatchListNotFoundException("WatchList not Found."));

        if (!watchList.getUser().getId().equals(user.getId())){
            throw new UnauthorizedWatchListAccessException("You do not own this WatchList.");
        }

        if (watchListRepository.existsByUserAndNameIgnoreCase(user, newName)){
            throw new IllegalArgumentException("A WatchList with this name Already Exists");
        }

        watchList.setName(newName);
        watchListRepository.save(watchList);

        return mapToWatchListDTO(watchList);
    }

    public List<WatchListDTO> getAllWatchLists(Users user) {
        List<WatchList> watchLists = watchListRepository.findAllByUser(user);

        if (watchLists == null || watchLists.isEmpty()){
            return List.of();
        }

        return watchLists.stream().map(w -> mapToWatchListDTO(w)).toList();
    }

    public WatchListDTO addMediaToWatchList(Users user, Long watchListId, Long tmdbId) {
        WatchList watchList = watchListRepository.findById(watchListId).orElseThrow(() -> new WatchListNotFoundException("WatchList does not exist."));

        if(!watchList.getUser().getId().equals(user.getId())){
            throw new UnauthorizedWatchListAccessException("You do not own this WatchList");
        }
        
        Media media = mediaRepository.findByTmdbId(tmdbId).orElseThrow(() -> new MediaNotFoundException("Media does not exist."));

        boolean alreadyExists = watchList.getItems().stream().anyMatch(item -> item.getMedia().getId().equals(media.getId()));

        if (alreadyExists){
            throw new DuplicateWatchListMediaException("Media already exists in current WatchList");
        }

        WatchListItem newItem = WatchListItem.builder()
        .watchList(watchList)
        .media(media)
        .addedAt(LocalDateTime.now())
        .build();

        watchList.getItems().add(newItem);
        watchListRepository.save(watchList);

        return mapToWatchListDTO(watchList);
    }   

    public WatchListDTO removeMediaFromWatchList(Users user, Long watchListId, Long tmdbId) {
        WatchList watchList = watchListRepository.findById(watchListId).orElseThrow(() -> new WatchListNotFoundException("WatchList does not exist."));

        if(!watchList.getUser().getId().equals(user.getId())){
            throw new UnauthorizedWatchListAccessException("You do not own this WatchList");
        }

        Media media = mediaRepository.findByTmdbId(tmdbId).orElseThrow(() -> new MediaNotFoundException("Media does not exist."));

        WatchListItem itemToRemove = watchList.getItems().stream()
        .filter(item -> item.getMedia().getId().equals(media.getId()))
        .findFirst()
        .orElseThrow(() -> new MediaNotInWatchListException("Media is not in this watchlist"));

        watchList.getItems().remove(itemToRemove);
        watchListRepository.save(watchList);

        return mapToWatchListDTO(watchList);
    }

    @Transactional
    public WatchListDTO mapToWatchListDTO(WatchList watchList){
        Users user = watchList.getUser();

        List<MediaDetailsDTO> mediaDTO = watchList.getItems().stream().map(item -> {
            Media m = item.getMedia();
            List<Review> reviews = reviewsRepo.findByMedia(m);
            boolean isFavourited = user.getFavorites().contains(m);
            WatchStatus status = userMediaStatusRepository
            .findByUserAndMedia(user, m)
            .map(UserMediaStatus::getStatus)
            .orElse(WatchStatus.NONE);

            return watchMateMapper.mapToMediaDetailsDTO(m, reviews, isFavourited, status);
        }).toList();
        
        return watchMateMapper.mapToWatchListDTO(watchList, mediaDTO);
    }
}
