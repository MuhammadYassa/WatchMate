package com.project.watchmate.Services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.Dto.MovieDetailsDTO;
import com.project.watchmate.Mappers.WatchMateMapper;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.Review;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.ReviewRepository;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.UserShowTrackingRepository;
import com.project.watchmate.Repositories.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaResolutionService mediaResolutionService;

    private final MediaRepository mediaRepository;

    private final UsersRepository usersRepository;

    private final WatchMateMapper watchMateMapper;

    private final ReviewRepository reviewRepository;

    private final UserMediaStatusRepository userMediaStatusRepository;

    private final UserShowTrackingRepository userShowTrackingRepository;

    private final UserWatchStatusResolver userWatchStatusResolver;

    private final TmdbService tmdbService;

    @Transactional
    public MovieDetailsDTO getMovieDetails(Long tmdbId, Users userParam){
        Media media = userParam == null
            ? mediaRepository.findByTmdbIdAndType(tmdbId, MediaType.MOVIE)
                .orElseGet(() -> tmdbService.fetchMediaByTmdbId(tmdbId, MediaType.MOVIE))
            : mediaResolutionService.resolveMediaByTmdbId(tmdbId, MediaType.MOVIE);

        List<Review> reviews = media.getId() == null ? List.of() : reviewRepository.findByMedia(media);
        UserContext userContext = resolveUserContext(userParam, media);

        return watchMateMapper.mapToMovieDetailsDTO(media, reviews, userContext.isFavourited(), userContext.watchStatus());
    }

    public Page<Media> getMoviesWatchedPage(Users user){
        Pageable pageable = PageRequest.of(0, 5);
        return userMediaStatusRepository.findWatchedMoviesByUser(user, pageable);
    }

    public Page<Media> getShowsWatchedPage(Users user){
        Pageable pageable = PageRequest.of(0, 5);
        return userShowTrackingRepository.findWatchedShowsByUser(user, pageable);
    }

    public long countMoviesWatched(Users user) {
        return userMediaStatusRepository.countWatchedMoviesByUser(user);
    }

    public long countShowsWatched(Users user) {
        return userShowTrackingRepository.countWatchedShowsByUser(user);
    }

    private UserContext resolveUserContext(Users userParam, Media media) {
        if (userParam == null) {
            return new UserContext(false, WatchStatus.NONE);
        }

        Long userId = userParam.getId();
        Users user = usersRepository.findByIdWithFavorites(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isFavourited = user.getFavorites().contains(media);
        WatchStatus watchStatus = userWatchStatusResolver.resolveWatchStatus(user, media);

        return new UserContext(isFavourited, watchStatus);
    }

    private record UserContext(boolean isFavourited, WatchStatus watchStatus) {
    }
}
