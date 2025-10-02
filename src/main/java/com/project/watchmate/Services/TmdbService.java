package com.project.watchmate.Services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.project.watchmate.Dto.TmdbGenreDTO;
import com.project.watchmate.Dto.TmdbGenreResponseDTO;
import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Dto.TmdbResponseDTO;
import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Models.Genre;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.PopularMedia;
import com.project.watchmate.Repositories.GenreRepository;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.PopularMediaRepository;

import io.jsonwebtoken.lang.Collections;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TmdbService {

    private final WebClient tmdbWebClient;

    private final MediaRepository mediaRepository;

    private final PopularMediaRepository  popularMediaRepository;
    
    private final GenreRepository genreRepository;

    @PostConstruct
    public void syncGenres(){
        List<TmdbGenreDTO> movieGenres = fetchGenres("movie");
        List<TmdbGenreDTO> showGenres = fetchGenres("tv");

        Map<Long, String> genreMap = new HashMap<>();
        for (TmdbGenreDTO dto : movieGenres){
            genreMap.put(dto.getId(), dto.getName());
        }
        for (TmdbGenreDTO dto : showGenres){
            genreMap.put(dto.getId(), dto.getName());
        }

        for (Map.Entry<Long, String> entry : genreMap.entrySet()){
            if (!genreRepository.existsById(entry.getKey())){

                Genre genre = Genre.builder()
                .id(entry.getKey())
                .name(entry.getValue())
                .build();

                genreRepository.save(genre);
            }
        }
    }

    public List<TmdbGenreDTO> fetchGenres(String type){
        try{
            TmdbGenreResponseDTO response = tmdbWebClient.get()
            .uri("/genre/{type}/list", type)
            .retrieve()
            .bodyToMono(TmdbGenreResponseDTO.class)
            .block();

            return response != null ? response.getGenres() : Collections.emptyList();
        }
        catch (Exception e){
            return Collections.emptyList();
        }
    }

    @Scheduled(cron = "0 47 23 * * *")
    public void popularMedia(){
        fetchAndStorePopularMedia();
    }

    public void fetchAndStorePopularMedia(){
        popularMediaRepository.deleteAll();
        fetchAndStorePopular("tv", MediaType.SHOW);
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
            .releaseDate(TmdbMovieDTO.parseDate(item.getReleaseDate()).orElse(null))
            .rating(item.getVoteAverage())
            .type(mediaType)
            .build()).toList();

        List<Media> media = saveAndUpdateMedia(mediaList);

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

    public Media fetchMediaByTmdbId(Long tmdbId, MediaType type){
        String typePath = (type == MediaType.MOVIE) ? "movie" : "tv";
        String uri = "/" + typePath + "/" + tmdbId + "?language=en-US";
        TmdbMovieDTO tmdbMedia = new TmdbMovieDTO();

        try {tmdbMedia = tmdbWebClient.get()
        .uri(uri)
        .retrieve()
        .bodyToMono(TmdbMovieDTO.class)
        .blockOptional()
        .orElseThrow(() -> new MediaNotFoundException("TMDB media not found for ID: " + tmdbId)); 
        } catch (WebClientResponseException.NotFound ex) {
        throw new MediaNotFoundException("TMDB media not found for ID: " + tmdbId);
    }

        List<Long> genreIdsList = tmdbMedia.getGenres().stream().map(g -> g.getId()).toList();

        List<Genre> genreList = genreRepository.findAllById(genreIdsList);

        return Media.builder()
            .tmdbId(tmdbMedia.getId())
            .title(tmdbMedia.getTitle())
            .overview(tmdbMedia.getOverview())
            .posterPath(tmdbMedia.getPosterPath())
            .releaseDate(TmdbMovieDTO.parseDate(tmdbMedia.getReleaseDate()).orElse(null))
            .rating(tmdbMedia.getVoteAverage())
            .genres(genreList)
            .type(type)
            .build();
    }
}
