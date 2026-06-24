package com.project.watchmate.favourite.application;

import com.project.watchmate.media.catalog.application.MediaResolutionService;
import com.project.watchmate.media.catalog.application.UserWatchStatusResolver;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.common.cache.WatchMateCacheEvictionService;
import com.project.watchmate.favourite.dto.FavouriteStatusDTO;
import com.project.watchmate.media.catalog.dto.MediaDetailsDTO;
import com.project.watchmate.common.error.UserNotFoundException;
import com.project.watchmate.common.mapper.WatchMateMapper;
import com.project.watchmate.common.error.DuplicateFavouriteException;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.review.domain.Review;
import com.project.watchmate.review.persistence.ReviewRepository;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.user.persistence.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavouriteService {

    private final MediaResolutionService mediaResolutionService;

    private final UsersRepository usersRepository;

    private final WatchMateMapper watchMateMapper;

    private final UserWatchStatusResolver userWatchStatusResolver;

    private final WatchMateCacheEvictionService cacheEvictionService;

    private final ReviewRepository reviewRepository;

    @Transactional
    public FavouriteStatusDTO addToFavourites (Long tmdbId, String type, Users user){
        Users managedUser = loadUserWithFavorites(user);
        Media media = mediaResolutionService.resolveMediaByTmdbId(tmdbId, type);

        if (managedUser.getFavorites().contains(media)){
            throw new DuplicateFavouriteException ("Media has already been favourited.");
        }

        managedUser.getFavorites().add(media);
        cacheEvictionService.evictFavoriteCaches(managedUser.getId());
        return FavouriteStatusDTO.builder().tmdbId(tmdbId).isFavourited(managedUser.getFavorites().contains(media)).build();
    }

    @Transactional
    public FavouriteStatusDTO removeFromFavourites (Long tmdbId, String type, Users user){
        Users managedUser = loadUserWithFavorites(user);
        Media media = mediaResolutionService.resolveMediaByTmdbId(tmdbId, type);
        if (managedUser.getFavorites().contains(media)){
            managedUser.getFavorites().remove(media);
        }
        cacheEvictionService.evictFavoriteCaches(managedUser.getId());
        return FavouriteStatusDTO.builder().tmdbId(tmdbId).isFavourited(managedUser.getFavorites().contains(media)).build();
    }

    @Transactional(readOnly = true)
    public FavouriteStatusDTO isFavourited (Long tmdbId, String type, Users user){
        Media media = mediaResolutionService.resolveMediaByTmdbId(tmdbId, type);
        boolean favourited = usersRepository.isFavouritedByUser(user.getId(), media.getId());
        return FavouriteStatusDTO.builder().tmdbId(tmdbId).isFavourited(favourited).build();
    }

    @Transactional(readOnly = true)
    public Page<MediaDetailsDTO> getUserFavourites(Users user, int page, int size) {
        int cappedSize = Math.min(size, 50);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by("id").descending());
        Page<Media> mediaPage = usersRepository.findFavoritesByUserId(user.getId(), pageable);

        List<Media> mediaItems = mediaPage.getContent();
        if (mediaItems.isEmpty()) {
            return mediaPage.map(m -> watchMateMapper.mapToMediaDetailsDTO(m, List.of(), true, WatchStatus.NONE));
        }

        // Batch load watch statuses — 2 queries instead of N.
        Map<Long, WatchStatus> statusMap = userWatchStatusResolver.resolveWatchStatusBatch(user, mediaItems);

        // Batch load reviews — 1 query instead of N.
        List<Long> mediaIds = mediaItems.stream().map(Media::getId).collect(Collectors.toList());
        List<Review> allReviews = reviewRepository.findAllByMediaIdInWithUserAndMedia(mediaIds);
        Map<Long, List<Review>> reviewsByMediaId = allReviews.stream()
            .collect(Collectors.groupingBy(r -> r.getMedia().getId()));

        return mediaPage.map(m -> watchMateMapper.mapToMediaDetailsDTO(
            m,
            reviewsByMediaId.getOrDefault(m.getId(), List.of()),
            true,
            statusMap.getOrDefault(m.getId(), WatchStatus.NONE)
        ));
    }

    private Users loadUserWithFavorites(Users user) {
        Long userId = Objects.requireNonNull(Objects.requireNonNull(user, "user").getId(), "user.id");
        return usersRepository.findByIdWithFavorites(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

}






