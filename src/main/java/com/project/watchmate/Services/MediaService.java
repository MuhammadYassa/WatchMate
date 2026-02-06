package com.project.watchmate.Services;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.Dto.MediaDetailsDTO;
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
        Long id = Objects.requireNonNull(tmdbId, "tmdbId");
        Long userId = Objects.requireNonNull(Objects.requireNonNull(userParam, "userParam").getId(), "userParam.id");
        Users user = usersRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

        Media media = Objects.requireNonNull(mediaRepository.findByTmdbId(id).orElseGet(() -> tmdbService.fetchMediaByTmdbId(id, type)), "media");
        mediaRepository.save(media);

        List<Review> reviews = reviewRepository.findByMedia(media);

        boolean isFavourited = user.getFavorites().contains(media);

        UserMediaStatus userStatus= userMediaStatusRepository.findByUserAndMedia(user, media).orElse(null);
        WatchStatus watchStatus = userStatus != null ? userStatus.getStatus() : WatchStatus.NONE;


        return watchMateMapper.mapToMediaDetailsDTO(media, reviews, isFavourited, watchStatus);
    }

    public Page<Media> getMoviesWatchedPage(Users user){
        Pageable pageable = PageRequest.of(0, 5, Sort.by("releaseDate").descending().and(Sort.by("title")).descending());
        return userMediaStatusRepository.findWatchedMoviesByUser(user, pageable);
    }

    public Page<Media> getShowsWatchedPage(Users user){
        Pageable pageable = PageRequest.of(0, 5, Sort.by("releaseDate").descending().and(Sort.by("title")).descending());
        return userMediaStatusRepository.findWatchedShowsByUser(user, pageable);
    }
}
