package com.project.watchmate.movie.application;

import com.project.watchmate.media.catalog.application.MediaResolutionService;
import com.project.watchmate.media.catalog.application.UserWatchStatusResolver;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.movie.dto.MovieDetailsDTO;
import com.project.watchmate.movie.dto.PublicMovieDetailBaseDTO;
import com.project.watchmate.common.error.UserNotFoundException;
import com.project.watchmate.common.mapper.WatchMateMapper;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.media.extras.application.MediaExtrasService;
import com.project.watchmate.media.extras.dto.MediaExtrasDTO;
import com.project.watchmate.review.domain.Review;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.media.catalog.persistence.MediaRepository;
import com.project.watchmate.review.persistence.ReviewRepository;
import com.project.watchmate.movie.tracking.persistence.UserMediaStatusRepository;
import com.project.watchmate.show.tracking.persistence.UserShowTrackingRepository;
import com.project.watchmate.user.persistence.UsersRepository;

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

    private final PublicMediaDetailBaseCacheService publicMediaDetailBaseCacheService;

    private final MediaExtrasService mediaExtrasService;

    @Transactional
    public MovieDetailsDTO getMovieDetails(Long tmdbId, Users userParam){
        Media media = userParam == null
            ? mediaRepository.findByTmdbIdAndType(tmdbId, MediaType.MOVIE).orElse(null)
            : mediaResolutionService.resolveMediaByTmdbId(tmdbId, MediaType.MOVIE);

        PublicMovieDetailBaseDTO publicBase = publicMediaDetailBaseCacheService.getMovieBase(tmdbId, MediaType.MOVIE);
        List<Review> reviews = media == null || media.getId() == null ? List.of() : reviewRepository.findByMedia(media);
        UserContext userContext = resolveUserContext(userParam, media);
        MediaExtrasDTO extras = mediaExtrasService.getExtras(tmdbId, MediaType.MOVIE);

        return toMovieDetailsDTO(publicBase, reviews, userContext.isFavourited(), userContext.watchStatus(), extras);
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
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        boolean isFavourited = user.getFavorites().contains(media);
        WatchStatus watchStatus = userWatchStatusResolver.resolveWatchStatus(user, media);

        return new UserContext(isFavourited, watchStatus);
    }

    private MovieDetailsDTO toMovieDetailsDTO(
        PublicMovieDetailBaseDTO publicBase,
        List<Review> reviews,
        Boolean isFavourited,
        WatchStatus watchStatus,
        MediaExtrasDTO extras
    ) {
        return MovieDetailsDTO.builder()
            .tmdbId(publicBase.getTmdbId())
            .title(publicBase.getTitle())
            .posterPath(publicBase.getPosterPath())
            .backdropPath(publicBase.getBackdropPath())
            .overview(publicBase.getOverview())
            .releaseDate(publicBase.getReleaseDate())
            .rating(publicBase.getRating())
            .type(publicBase.getType())
            .genres(publicBase.getGenres())
            .reviews(reviews.stream().map(watchMateMapper::mapToReviewDTO).toList())
            .isFavourited(isFavourited)
            .watchStatus(watchStatus)
            .cast(extras.cast())
            .bestTrailer(extras.bestTrailer())
            .watchProviders(extras.watchProviders())
            .build();
    }

    private record UserContext(boolean isFavourited, WatchStatus watchStatus) {
    }
}








