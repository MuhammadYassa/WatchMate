package com.project.watchmate.Services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.watchmate.Dto.FavouriteStatusDTO;
import com.project.watchmate.Dto.UserFavouritesDTO;
import com.project.watchmate.Dto.MediaDetailsDTO;
import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Mappers.WatchMateMapper;
import com.project.watchmate.Exception.DuplicateFavouriteException;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavouriteService {

    private final MediaRepository mediaRepository;

    private final UsersRepository usersRepository;

    private final WatchMateMapper watchMateMapper;

    private final UserMediaStatusRepository userMediaStatusRepository;

    public FavouriteStatusDTO addToFavourites (Long tmdbId, Users user){
        Media media = mediaRepository.findByTmdbId(tmdbId).orElseThrow(() -> new MediaNotFoundException("Media does not exist!"));

        if (user.getFavorites().contains(media)){
            throw new DuplicateFavouriteException ("Media has already been favourited.");
        }

        user.getFavorites().add(media);
        usersRepository.save(user);
        return FavouriteStatusDTO.builder().tmdbId(tmdbId).isFavourited(user.getFavorites().contains(media)).build();
    }

    public FavouriteStatusDTO removeFromFavourites (Long tmdbId, Users user){
        Media media = mediaRepository.findByTmdbId(tmdbId).orElseThrow(() -> new MediaNotFoundException("Media does not exist!"));
        if (user.getFavorites().contains(media)){
            user.getFavorites().remove(media);
        }
        usersRepository.save(user);
        return FavouriteStatusDTO.builder().tmdbId(tmdbId).isFavourited(user.getFavorites().contains(media)).build();
    }

    public FavouriteStatusDTO isFavourited (Long tmdbId, Users user){
        Media media = mediaRepository.findByTmdbId(tmdbId).orElseThrow(() -> new MediaNotFoundException("Media does not exist!"));
        return FavouriteStatusDTO.builder().tmdbId(tmdbId).isFavourited(user.getFavorites().contains(media)).build();
    }

    public UserFavouritesDTO getUserFavourites (Users user){
        List<Media> allMedia = user.getFavorites();
        List<MediaDetailsDTO> allMediaDetails = allMedia.stream().map(m -> {
            WatchStatus watchStatus = userMediaStatusRepository
                .findByUserAndMedia(user, m)
                .map(UserMediaStatus::getStatus)
                .orElse(WatchStatus.NONE);
            boolean isFavourited = user.getFavorites().contains(m);

            return watchMateMapper.mapToMediaDetailsDTO(m, m.getReviews(), isFavourited, watchStatus);
        }).toList();

        return UserFavouritesDTO.builder()
            .favourites(allMediaDetails)
            .totalCount(allMediaDetails.size())
            .build();
    }
}
