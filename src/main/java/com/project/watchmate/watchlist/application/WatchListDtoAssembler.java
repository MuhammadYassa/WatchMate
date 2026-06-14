package com.project.watchmate.watchlist.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.common.mapper.WatchMateMapper;
import com.project.watchmate.favourite.application.UserFavoriteMediaIdsCacheService;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.media.catalog.dto.MediaDetailsDTO;
import com.project.watchmate.movie.tracking.domain.UserMediaStatus;
import com.project.watchmate.movie.tracking.persistence.UserMediaStatusRepository;
import com.project.watchmate.review.domain.Review;
import com.project.watchmate.review.persistence.ReviewRepository;
import com.project.watchmate.show.tracking.domain.UserShowTracking;
import com.project.watchmate.show.tracking.persistence.UserShowTrackingRepository;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.watchlist.domain.WatchList;
import com.project.watchmate.watchlist.domain.WatchListItem;
import com.project.watchmate.watchlist.dto.WatchListDTO;
import com.project.watchmate.watchlist.persistence.WatchListItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WatchListDtoAssembler {

    private final WatchMateMapper watchMateMapper;

    private final WatchListItemRepository watchListItemRepository;

    private final ReviewRepository reviewsRepo;

    private final UserMediaStatusRepository userMediaStatusRepository;

    private final UserShowTrackingRepository userShowTrackingRepository;

    private final UserFavoriteMediaIdsCacheService userFavoriteMediaIdsCacheService;

    @Transactional
    public WatchListDTO mapToWatchListDTO(WatchList watchList) {
        List<WatchListDTO> mapped = mapWatchLists(watchList.getUser(), List.of(watchList));
        return mapped.isEmpty()
            ? watchMateMapper.mapToWatchListDTO(watchList, List.of())
            : mapped.get(0);
    }

    @Transactional(readOnly = true)
    public List<WatchListDTO> mapWatchLists(Users user, List<WatchList> watchLists) {
        if (watchLists == null || watchLists.isEmpty()) {
            return List.of();
        }

        BatchWatchListMappingData mappingData = loadBatchMappingData(user, watchLists);

        return watchLists.stream()
            .map(watchList -> {
                List<MediaDetailsDTO> mediaDTOs = mappingData.itemsByWatchListId()
                    .getOrDefault(watchList.getId(), List.of())
                    .stream()
                    .map(item -> mapWatchListItem(item, mappingData))
                    .toList();

                return watchMateMapper.mapToWatchListDTO(watchList, mediaDTOs);
            })
            .toList();
    }

    private MediaDetailsDTO mapWatchListItem(WatchListItem item, BatchWatchListMappingData mappingData) {
        Media media = item.getMedia();
        Long mediaId = media.getId();
        List<Review> reviews = mappingData.reviewsByMediaId().getOrDefault(mediaId, List.of());
        boolean isFavourited = mappingData.favoriteMediaIds().contains(mediaId);
        WatchStatus status = resolveBatchWatchStatus(media, mappingData);

        return watchMateMapper.mapToMediaDetailsDTO(media, reviews, isFavourited, status);
    }

    private WatchStatus resolveBatchWatchStatus(Media media, BatchWatchListMappingData mappingData) {
        if (media == null || media.getId() == null) {
            return WatchStatus.NONE;
        }

        Map<Long, WatchStatus> statuses = media.getType() == MediaType.SHOW
            ? mappingData.showStatusesByMediaId()
            : mappingData.movieStatusesByMediaId();

        return statuses.getOrDefault(media.getId(), WatchStatus.NONE);
    }

    private BatchWatchListMappingData loadBatchMappingData(Users user, List<WatchList> watchLists) {
        List<Long> watchListIds = watchLists.stream()
            .map(WatchList::getId)
            .filter(Objects::nonNull)
            .toList();

        List<WatchListItem> items = watchListIds.isEmpty()
            ? List.of()
            : nullToEmpty(watchListItemRepository.findAllByWatchListIdInWithMediaAndGenres(watchListIds));

        Map<Long, List<WatchListItem>> itemsByWatchListId = groupItemsByWatchListId(items);
        Set<Long> mediaIds = collectMediaIds(items);

        Map<Long, List<Review>> reviewsByMediaId = mediaIds.isEmpty()
            ? Map.of()
            : nullToEmpty(reviewsRepo.findAllByMediaIdInWithUserAndMedia(mediaIds)).stream()
                .filter(review -> review.getMedia() != null && review.getMedia().getId() != null)
                .collect(Collectors.groupingBy(review -> review.getMedia().getId()));

        Set<Long> favoriteMediaIds = mediaIds.isEmpty() || user == null || user.getId() == null
            ? Set.of()
            : userFavoriteMediaIdsCacheService.getFavoriteMediaIds(user.getId()).stream()
                .filter(mediaIds::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, WatchStatus> movieStatusesByMediaId = mediaIds.isEmpty() || user == null
            ? Map.of()
            : nullToEmpty(userMediaStatusRepository.findByUserAndMediaIdIn(user, mediaIds)).stream()
                .filter(status -> status.getMedia() != null && status.getMedia().getId() != null)
                .filter(status -> status.getStatus() != null)
                .collect(Collectors.toMap(status -> status.getMedia().getId(), UserMediaStatus::getStatus, (first, second) -> first));

        Map<Long, WatchStatus> showStatusesByMediaId = mediaIds.isEmpty() || user == null
            ? Map.of()
            : nullToEmpty(userShowTrackingRepository.findByUserAndMediaIdIn(user, mediaIds)).stream()
                .filter(tracking -> tracking.getMedia() != null && tracking.getMedia().getId() != null)
                .filter(tracking -> tracking.getStatus() != null)
                .collect(Collectors.toMap(tracking -> tracking.getMedia().getId(), UserShowTracking::getStatus, (first, second) -> first));

        return new BatchWatchListMappingData(
            itemsByWatchListId,
            reviewsByMediaId,
            favoriteMediaIds,
            movieStatusesByMediaId,
            showStatusesByMediaId
        );
    }

    private Map<Long, List<WatchListItem>> groupItemsByWatchListId(List<WatchListItem> items) {
        Map<Long, List<WatchListItem>> itemsByWatchListId = new LinkedHashMap<>();
        for (WatchListItem item : items) {
            if (item.getWatchList() == null || item.getWatchList().getId() == null) {
                continue;
            }
            itemsByWatchListId
                .computeIfAbsent(item.getWatchList().getId(), ignored -> new ArrayList<>())
                .add(item);
        }
        return itemsByWatchListId;
    }

    private Set<Long> collectMediaIds(List<WatchListItem> items) {
        return items.stream()
            .map(WatchListItem::getMedia)
            .filter(Objects::nonNull)
            .map(Media::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record BatchWatchListMappingData(
        Map<Long, List<WatchListItem>> itemsByWatchListId,
        Map<Long, List<Review>> reviewsByMediaId,
        Set<Long> favoriteMediaIds,
        Map<Long, WatchStatus> movieStatusesByMediaId,
        Map<Long, WatchStatus> showStatusesByMediaId
    ) {
    }
}
