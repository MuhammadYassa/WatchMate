package com.project.watchmate.Mappers;

import java.util.List;

import org.springframework.stereotype.Component;

import com.project.watchmate.Dto.MediaDetailsDTO;
import com.project.watchmate.Dto.ReviewDTO;
import com.project.watchmate.Dto.WatchListDTO;
import com.project.watchmate.Models.Genre;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.Review;
import com.project.watchmate.Models.WatchList;
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

    public WatchListDTO mapToWatchListDTO(WatchList watchList, List<MediaDetailsDTO> mediaDTOs) {
        return WatchListDTO.builder()
            .id(watchList.getId())
            .name(watchList.getName())
            .media(mediaDTOs)
            .build();
    }

    public ReviewDTO mapToReviewDTO(Review review){
        return ReviewDTO.builder()
        .username(review.getUser().getUsername())
        .comment(review.getComment())
        .starRating(review.getRating())
        .postedAt(review.getDatePosted())
        .build();
    }
}
