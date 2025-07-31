package com.project.watchmate.Services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.watchmate.Dto.MediaDetailsDTO;
import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Mappers.WatchMateMapper;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.Review;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.ReviewRepository;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.UsersRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;

    private final UsersRepository usersRepository;

    private final TmdbService tmdbService;

    private final WatchMateMapper watchMateMapper;

    private final ReviewRepository reviewRepository;

    private final UserMediaStatusRepository userMediaStatusRepository;

    @Transactional
    public MediaDetailsDTO getMediaDetails(Long tmdbId, MediaType type, Users userParam){

        Users user = usersRepository.findById(userParam.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

        Media media = mediaRepository.findByTmdbIdAndType(tmdbId, type).orElse(media = tmdbService.fetchMediaByTmdbId(tmdbId, type));

        if (media == null){
            throw new MediaNotFoundException("Media not found for TMDB ID:" + tmdbId);
        }

        mediaRepository.save(media);

        List<Review> reviews = reviewRepository.findByMedia(media);

        boolean isFavourited = user.getFavorites().contains(media);

        UserMediaStatus userStatus= userMediaStatusRepository.findByUserAndMedia(user, media).orElse(null);
        WatchStatus watchStatus = userStatus != null ? userStatus.getStatus() : WatchStatus.NONE;


        return watchMateMapper.mapToMediaDetailsDTO(media, reviews, isFavourited, watchStatus);
    }
}
