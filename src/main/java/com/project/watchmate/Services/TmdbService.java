package com.project.watchmate.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Dto.TmdbResponseDTO;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.PopularMedia;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.PopularMediaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TmdbService {

    private final WebClient tmdbWebClient;

    private final MediaRepository mediaRepository;

    private final PopularMediaRepository  popularMediaRepository;

    @Scheduled(cron = "0 0 2 * * *")
    public void popularMedia(){
        fetchAndStorePopularMedia();
    }

    public void fetchAndStorePopularMedia(){
        fetchAndStorePopular("tv", MediaType.TV_SHOW);
        fetchAndStorePopular("movie", MediaType.MOVIE);
    }

    @Transactional
    public void fetchAndStorePopular(String type, MediaType mediaType){
        String uri = "/" + type + "/popular?language=en-US&page=1";

        List<TmdbMovieDTO> results = tmdbWebClient.get()
        .uri(uri)
        .retrieve()
        .bodyToMono(TmdbResponseDTO.class)
        .blockOptional()
        .map(TmdbResponseDTO::getResults)
        .orElse(List.of());

        List<Media> mediaList = results.stream()
        .map(item -> Media.builder()
            .tmdbId((long)item.getId())
            .title(item.getTitle())
            .overview(item.getOverview())
            .posterPath(item.getPosterPath())
            .releaseDate(TmdbMovieDTO.parseDate(item.getReleaseDate()))
            .rating(item.getVoteAverage())
            .type(mediaType)
            .build()).toList();

        List<Media> media = saveAndUpdateMedia(mediaList);

        popularMediaRepository.deleteAll();

        List<PopularMedia> popularList = new ArrayList<>();

        for (int i = 0; i < media.size(); i++){
            popularList.add(PopularMedia.builder()
            .media(media.get(i))
            .popularityRank(i+1)
            .build());
        }
        popularMediaRepository.saveAll(popularList);
    }

    public List<Media> saveAndUpdateMedia(List<Media> mediaList){

        List<Media> savedMedia = mediaList.stream()
        .map(media -> {
        return mediaRepository.findByTmdbId(media.getTmdbId())
            .map(existing -> {
                    existing.setTitle(media.getTitle());
                    existing.setOverview(media.getOverview());
                    existing.setPosterPath(media.getPosterPath());
                    existing.setReleaseDate(media.getReleaseDate());
                    existing.setRating(media.getRating());
                    existing.setType(media.getType());
                    return mediaRepository.save(existing);
                })
                .orElseGet(() -> mediaRepository.save(media));
        })
        .toList();

        return savedMedia;
    }

}
