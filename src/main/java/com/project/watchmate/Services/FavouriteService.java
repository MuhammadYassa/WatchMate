package com.project.watchmate.Services;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.Dto.FavouriteStatusDTO;
import com.project.watchmate.Dto.UserFavouritesDTO;
import com.project.watchmate.Dto.MediaDetailsDTO;
import com.project.watchmate.Exception.UserNotFoundException;
import com.project.watchmate.Mappers.WatchMateMapper;
import com.project.watchmate.Exception.DuplicateFavouriteException;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavouriteService {

    private final MediaResolutionService mediaResolutionService;

    private final UsersRepository usersRepository;

    private final WatchMateMapper watchMateMapper;

    private final UserMediaStatusRepository userMediaStatusRepository;

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
            WatchStatus watchStatus = userMediaStatusRepository
                .findByUserAndMedia(managedUser, m)
                .map(UserMediaStatus::getStatus)
                .orElse(WatchStatus.NONE);
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
