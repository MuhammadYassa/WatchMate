package com.project.watchmate.favourite.application;

import com.project.watchmate.media.catalog.application.MediaResolutionService;
import com.project.watchmate.media.catalog.application.UserWatchStatusResolver;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.favourite.dto.FavouriteStatusDTO;
import com.project.watchmate.favourite.dto.UserFavouritesDTO;
import com.project.watchmate.media.catalog.dto.MediaDetailsDTO;
import com.project.watchmate.common.error.UserNotFoundException;
import com.project.watchmate.common.mapper.WatchMateMapper;
import com.project.watchmate.common.error.DuplicateFavouriteException;
import com.project.watchmate.media.catalog.domain.Media;
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

    @Transactional
    public FavouriteStatusDTO addToFavourites (Long tmdbId, String type, Users user){
        Users managedUser = loadUserWithFavorites(user);
        Media media = mediaResolutionService.resolveMediaByTmdbId(tmdbId, type);

        if (managedUser.getFavorites().contains(media)){
            throw new DuplicateFavouriteException ("Media has already been favourited.");
        }

        managedUser.getFavorites().add(media);
        return FavouriteStatusDTO.builder().tmdbId(tmdbId).isFavourited(managedUser.getFavorites().contains(media)).build();
    }

    @Transactional
    public FavouriteStatusDTO removeFromFavourites (Long tmdbId, String type, Users user){
        Users managedUser = loadUserWithFavorites(user);
        Media media = mediaResolutionService.resolveMediaByTmdbId(tmdbId, type);
        if (managedUser.getFavorites().contains(media)){
            managedUser.getFavorites().remove(media);
        }
        return FavouriteStatusDTO.builder().tmdbId(tmdbId).isFavourited(managedUser.getFavorites().contains(media)).build();
    }

    @Transactional(readOnly = true)
    public FavouriteStatusDTO isFavourited (Long tmdbId, String type, Users user){
        Users managedUser = loadUserWithFavorites(user);
        Media media = mediaResolutionService.resolveMediaByTmdbId(tmdbId, type);
        return FavouriteStatusDTO.builder().tmdbId(tmdbId).isFavourited(managedUser.getFavorites().contains(media)).build();
    }

    @Transactional(readOnly = true)
    public UserFavouritesDTO getUserFavourites (Users user){
        Users managedUser = loadUserWithFavorites(user);
        List<Media> allMedia = managedUser.getFavorites();
        List<MediaDetailsDTO> allMediaDetails = allMedia.stream().map(m -> {
            WatchStatus watchStatus = userWatchStatusResolver.resolveWatchStatus(managedUser, m);
            boolean isFavourited = managedUser.getFavorites().contains(m);

            return watchMateMapper.mapToMediaDetailsDTO(m, m.getReviews(), isFavourited, watchStatus);
        }).toList();

        return UserFavouritesDTO.builder()
            .favourites(allMediaDetails)
            .totalCount(allMediaDetails.size())
            .build();
    }

    private Users loadUserWithFavorites(Users user) {
        Long userId = Objects.requireNonNull(Objects.requireNonNull(user, "user").getId(), "user.id");
        return usersRepository.findByIdWithFavorites(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}






