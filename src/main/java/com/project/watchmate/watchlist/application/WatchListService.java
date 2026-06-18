package com.project.watchmate.watchlist.application;

import com.project.watchmate.media.catalog.application.MediaResolutionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.common.cache.WatchMateCacheEvictionService;
import com.project.watchmate.watchlist.dto.WatchListDTO;
import com.project.watchmate.watchlist.dto.WatchListPageCacheDTO;
import com.project.watchmate.common.error.DuplicateWatchListMediaException;
import com.project.watchmate.common.error.MediaNotInWatchListException;
import com.project.watchmate.common.error.UnauthorizedWatchListAccessException;
import com.project.watchmate.common.error.WatchListNotFoundException;
import com.project.watchmate.common.error.WatchlistNameConflictException;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.user.domain.Role;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.watchlist.domain.WatchList;
import com.project.watchmate.watchlist.domain.WatchListItem;
import com.project.watchmate.watchlist.persistence.WatchListRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WatchListService {

    private final WatchListRepository watchListRepository;

    private final MediaResolutionService mediaResolutionService;

    private final WatchListDtoAssembler watchListDtoAssembler;

    private final WatchListPageCacheService watchListPageCacheService;

    private final WatchMateCacheEvictionService cacheEvictionService;

    public Page<WatchList> getWatchListPage(Users user){
        Pageable pageable = PageRequest.of(0, 5, Sort.by("name").descending().and(Sort.by("id").ascending()));
        return watchListRepository.findAllByUser(user, pageable);
    }
    
    @Transactional
    public WatchListDTO createWatchList(Users user, String name) {
        if (watchListRepository.existsByUserAndNameIgnoreCase(user, name)){
            throw new WatchlistNameConflictException("Watchlist Already Exists.");
        }
        WatchList watchList = Objects.requireNonNull(WatchList.builder()
        .name(name)
        .user(user)
        .build());

        saveWatchListOrThrowNameConflict(watchList, "Watchlist Already Exists.");

        cacheEvictionService.evictWatchlistSummaryPages();
        return watchListDtoAssembler.mapToWatchListDTO(watchList);
    }

    public void deleteWatchList(Users user, Long id) {
        WatchList watchList = watchListRepository.findById(Objects.requireNonNull(id, "id")).orElseThrow(() -> new WatchListNotFoundException("WatchList not found"));

        if (!watchList.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN){
            throw new UnauthorizedWatchListAccessException("You do not own this watchlist");
        }

        watchListRepository.delete(watchList);
        cacheEvictionService.evictWatchlistSummaryPages();
    }

    @Transactional
    public WatchListDTO renameWatchList(Users user, Long id, String newName) {
        WatchList watchList = watchListRepository.findById(Objects.requireNonNull(id, "id")).orElseThrow(() -> new WatchListNotFoundException("WatchList not found"));

        if (!watchList.getUser().getId().equals(user.getId())){
            throw new UnauthorizedWatchListAccessException("You do not own this WatchList.");
        }

        if (watchListRepository.existsByUserAndNameIgnoreCase(user, newName)){
            throw new WatchlistNameConflictException("A WatchList with this name Already Exists");
        }

        watchList.setName(newName);
        saveWatchListOrThrowNameConflict(watchList, "A WatchList with this name Already Exists");

        cacheEvictionService.evictWatchlistSummaryPages();
        return watchListDtoAssembler.mapToWatchListDTO(watchList);
    }

    private void saveWatchListOrThrowNameConflict(WatchList watchList, String conflictMessage) {
        try {
            watchListRepository.saveAndFlush(watchList);
        } catch (DataIntegrityViolationException ex) {
            throw new WatchlistNameConflictException(conflictMessage);
        }
    }

    @Transactional(readOnly = true)
    public Page<WatchListDTO> getAllWatchLists(Users user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        WatchListPageCacheDTO cachedPage = watchListPageCacheService.getAllWatchLists(user, page, size);
        return new PageImpl<>(cachedPage.getContent(), pageable, cachedPage.getTotalElements());
    }

    public WatchListDTO addMediaToWatchList(Users user, Long watchListId, Long tmdbId, String type) {
        WatchList watchList = watchListRepository.findById(Objects.requireNonNull(watchListId, "watchListId")).orElseThrow(() -> new WatchListNotFoundException("WatchList does not exist."));

        if(!watchList.getUser().getId().equals(user.getId())){
            throw new UnauthorizedWatchListAccessException("You do not own this WatchList");
        }
        
        Media media = mediaResolutionService.resolveMediaByTmdbId(tmdbId, type);

        boolean alreadyExists = watchList.getItems().stream().anyMatch(item -> item.getMedia().getId().equals(media.getId()));

        if (alreadyExists){
            throw new DuplicateWatchListMediaException("Media already exists in current WatchList");
        }

        WatchListItem newItem = Objects.requireNonNull(WatchListItem.builder()
        .watchList(watchList)
        .media(media)
        .addedAt(LocalDateTime.now())
        .build());

        watchList.getItems().add(newItem);
        watchListRepository.save(watchList);

        cacheEvictionService.evictWatchlistSummaryPages();
        return watchListDtoAssembler.mapToWatchListDTO(watchList);
    }   

    public WatchListDTO removeMediaFromWatchList(Users user, Long watchListId, Long tmdbId, String type) {
        WatchList watchList = watchListRepository.findById(Objects.requireNonNull(watchListId, "watchListId")).orElseThrow(() -> new WatchListNotFoundException("WatchList does not exist."));

        if(!watchList.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN){
            throw new UnauthorizedWatchListAccessException("You do not own this WatchList");
        }

        Media media = mediaResolutionService.resolveMediaByTmdbId(tmdbId, type);

        WatchListItem itemToRemove = watchList.getItems().stream()
        .filter(item -> item.getMedia().getId().equals(media.getId()))
        .findFirst()
        .orElseThrow(() -> new MediaNotInWatchListException("Media is not in this watchlist"));

        watchList.getItems().remove(itemToRemove);
        watchListRepository.save(watchList);

        cacheEvictionService.evictWatchlistSummaryPages();
        return watchListDtoAssembler.mapToWatchListDTO(watchList);
    }

    @Transactional
    public WatchListDTO mapToWatchListDTO(WatchList watchList){
        return watchListDtoAssembler.mapToWatchListDTO(watchList);
    }

    @Transactional(readOnly = true)
    public List<WatchListDTO> mapWatchListsForViewer(List<WatchList> watchLists, Users viewer) {
        return watchListDtoAssembler.mapWatchListsForViewer(watchLists, viewer);
    }
}






