package com.project.watchmate.watchlist.application;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.common.cache.WatchMateCacheNames;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.watchlist.domain.WatchList;
import com.project.watchmate.watchlist.dto.WatchListPageCacheDTO;
import com.project.watchmate.watchlist.persistence.WatchListRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WatchListPageCacheService {

    private final WatchListRepository watchListRepository;

    private final WatchListDtoAssembler watchListDtoAssembler;

    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames = WatchMateCacheNames.WATCHLIST_SUMMARY_PAGES,
        key = "T(com.project.watchmate.common.cache.WatchMateCacheKeys).watchlistPage(#user.id, #page, #size)",
        unless = "#result == null"
    )
    public WatchListPageCacheDTO getAllWatchLists(Users user, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<WatchList> watchLists = watchListRepository.findAllByUser(user, pageable);

        return WatchListPageCacheDTO.builder()
            .content(watchListDtoAssembler.mapWatchLists(user, watchLists.getContent()))
            .page(page)
            .size(size)
            .totalElements(watchLists.getTotalElements())
            .build();
    }
}
