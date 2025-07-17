package com.project.watchmate.Services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.watchmate.Dto.MediaDetailsDTO;
import com.project.watchmate.Dto.ReviewDTO;
import com.project.watchmate.Models.Genre;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.Review;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.ReviewRepository;
import com.project.watchmate.Repositories.UserMediaStatusRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;

    private final ReviewRepository reviewRepository;

    private final UserMediaStatusRepository userMediaStatusRepository;

    private final TmdbService tmdbService;

    @Transactional
    public MediaDetailsDTO getMediaDetails(Long tmdbId, MediaType type, Users user){
        Media media = mediaRepository.findByTmdbIdAndType(tmdbId, type).orElse(media = tmdbService.fetchMediaByTmdbId(tmdbId, type));

        if (media == null){
            throw new RuntimeException("Media not found in TMDB");
        }

        mediaRepository.save(media);

        List<Review> reviews = reviewRepository.findByMedia(media);
        List<ReviewDTO> reviewDTOs = reviews.stream().map(r -> ReviewDTO.builder()
        .username(r.getUser().getUsername())
        .starRating(r.getRating())
        .comment(r.getComment())
        .postedAt(r.getDatePosted())
        .build()).toList();

        List<String> genreNames = media.getGenres().stream().map(Genre::getName).toList();

        boolean isFavourited = user.getFavorites().contains(media);

        UserMediaStatus userStatus= userMediaStatusRepository.findByUserAndMedia(user, media).orElse(null);
        WatchStatus watchStatus = userStatus != null ? userStatus.getStatus() : WatchStatus.NONE;

        return MediaDetailsDTO.builder()
        .tmdbId(tmdbId)
        .title(media.getTitle())
        .overview(media.getOverview())
        .posterPath(media.getPosterPath())
        .releaseDate(media.getReleaseDate())
        .rating(media.getRating())
        .genres(genreNames)
        .reviews(reviewDTOs)
        .isFavourited(isFavourited)
        .watchStatus(watchStatus)
        .build();
    }
}
