package com.project.watchmate.common.mapper;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.social.dto.FollowRequestDTO;
import com.project.watchmate.social.dto.FollowListUserDetailsDTO;
import com.project.watchmate.discovery.dto.DiscoveryMediaItemDTO;
import com.project.watchmate.media.catalog.dto.MediaDetailsDTO;
import com.project.watchmate.movie.dto.MovieDetailsDTO;
import com.project.watchmate.review.dto.ReviewResponseDTO;
import com.project.watchmate.media.search.dto.SearchItemDTO;
import com.project.watchmate.media.tmdb.dto.TmdbMovieDTO;
import com.project.watchmate.watchlist.dto.WatchListDTO;
import com.project.watchmate.social.domain.FollowRequest;
import com.project.watchmate.media.catalog.domain.Genre;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.review.domain.Review;
import com.project.watchmate.watchlist.domain.WatchList;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WatchMateMapper {

    public MediaDetailsDTO mapToMediaDetailsDTO(Media media, List<Review> reviews, Boolean isFavourited, WatchStatus watchStatus) {
        return MediaDetailsDTO.builder()
            .tmdbId(media.getTmdbId())
            .title(media.getTitle())
            .posterPath(media.getPosterPath())
            .backdropPath(media.getBackdropPath())
            .overview(media.getOverview())
            .releaseDate(media.getReleaseDate())
            .rating(media.getRating())
            .type(media.getType())
            .genres(media.getGenres().stream().map(Genre::getName).toList())
            .reviews(reviews.stream().map(r -> mapToReviewDTO(r)).toList())
            .isFavourited(isFavourited)
            .watchStatus(watchStatus)
            .build();
    }

    public MovieDetailsDTO mapToMovieDetailsDTO(Media media, List<Review> reviews, Boolean isFavourited, WatchStatus watchStatus) {
        return MovieDetailsDTO.builder()
            .tmdbId(media.getTmdbId())
            .title(media.getTitle())
            .posterPath(media.getPosterPath())
            .backdropPath(media.getBackdropPath())
            .overview(media.getOverview())
            .releaseDate(media.getReleaseDate())
            .rating(media.getRating())
            .type(media.getType())
            .genres(media.getGenres().stream().map(Genre::getName).toList())
            .reviews(reviews.stream().map(this::mapToReviewDTO).toList())
            .isFavourited(isFavourited)
            .watchStatus(watchStatus)
            .build();
    }

    public SearchItemDTO mapToSearchItemDTO(Media m){
        return SearchItemDTO.builder()
            .id(m.getTmdbId())
            .title(m.getTitle())
            .posterPath(m.getPosterPath())
            .mediaType(m.getType().toString())
            .releaseDate(m.getReleaseDate() == null ? null : m.getReleaseDate().toString())
            .voteAverage(m.getRating())
            .overview(m.getOverview())
            .genres(m.getGenres().stream().map(Genre::getName).toList())
            .build();
    }

    public DiscoveryMediaItemDTO mapToDiscoveryMediaItemDTO(Media media) {
        return DiscoveryMediaItemDTO.builder()
            .tmdbId(media.getTmdbId())
            .title(media.getTitle())
            .overview(media.getOverview())
            .posterPath(media.getPosterPath())
            .backdropPath(media.getBackdropPath())
            .releaseDate(media.getReleaseDate())
            .rating(media.getRating())
            .type(media.getType())
            .build();
    }

    public DiscoveryMediaItemDTO mapToDiscoveryMediaItemDTO(TmdbMovieDTO media, MediaType mediaType) {
        return DiscoveryMediaItemDTO.builder()
            .tmdbId(media.getId())
            .title(media.getTitle())
            .overview(media.getOverview())
            .posterPath(media.getPosterPath())
            .backdropPath(media.getBackdropPath())
            .releaseDate(TmdbMovieDTO.parseDate(media.getReleaseDate()).orElse(null))
            .rating(media.getVoteAverage())
            .type(mediaType)
            .build();
    }

    public WatchListDTO mapToWatchListDTO(WatchList watchList, List<MediaDetailsDTO> mediaDTOs) {
        return WatchListDTO.builder()
            .id(watchList.getId())
            .name(watchList.getName())
            .media(mediaDTOs)
            .build();
    }

    public ReviewResponseDTO mapToReviewDTO(Review review){
        return mapToReviewResponseDTO(review);
    }

    public FollowListUserDetailsDTO mapToFollowListUserDetailsDTO(Users user) {
        return FollowListUserDetailsDTO.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .build();
    }

    public ReviewResponseDTO mapToReviewResponseDTO(Review review) {
        return ReviewResponseDTO.builder()
            .username(review.getUser().getUsername())
            .reviewId(review.getId())
            .tmdbId(review.getMedia().getTmdbId())
            .starRating(review.getRating())
            .comment(review.getComment())
            .postedAt(review.getDatePosted())
            .updatedAt(review.getDateLastModified())
            .build();
    }

    @Transactional
    public FollowRequestDTO mapToFollowRequestDTO(FollowRequest request) {
        Users requesterUser = request.getRequestUser();
        Users targetUser = request.getTargetUser();
        return FollowRequestDTO.builder()
            .requestId(request.getId())
            .requesterUserId(requesterUser.getId())
            .targetUserId(targetUser.getId())
            .requesterUsername(requesterUser.getUsername())
            .targetUsername(targetUser.getUsername())
            .requestedAt(request.getRequestedAt())
            .status(request.getStatus())
            .build();
    }
}






