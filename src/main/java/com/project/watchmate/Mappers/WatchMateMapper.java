package com.project.watchmate.Mappers;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.Dto.FollowRequestDTO;
import com.project.watchmate.Dto.FollowListUserDetailsDTO;
import com.project.watchmate.Dto.DiscoveryMediaItemDTO;
import com.project.watchmate.Dto.MediaDetailsDTO;
import com.project.watchmate.Dto.UserMediaStatusDTO;
import com.project.watchmate.Dto.ReviewResponseDTO;
import com.project.watchmate.Dto.SearchItemDTO;
import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Dto.WatchListDTO;
import com.project.watchmate.Models.FollowRequest;
import com.project.watchmate.Models.Genre;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.Review;
import com.project.watchmate.Models.WatchList;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WatchMateMapper {

    public MediaDetailsDTO mapToMediaDetailsDTO(Media media, List<Review> reviews, boolean isFavourited, WatchStatus watchStatus) {
        return MediaDetailsDTO.builder()
            .tmdbId(media.getTmdbId())
            .title(media.getTitle())
            .posterPath(media.getPosterPath())
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
            .username(user.getUsername())
            .build();
    }

    public UserMediaStatusDTO mapToUserMediaStatusDTO(UserMediaStatus userMediaStatus) {
        return UserMediaStatusDTO.builder()
            .tmdbId(userMediaStatus.getMedia().getTmdbId())
            .status(userMediaStatus.getStatus())
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
