package com.project.watchmate.Services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.Clients.TmdbClient;
import com.project.watchmate.Dto.TmdbGenreDTO;
import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Models.Genre;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.PopularMedia;
import com.project.watchmate.Repositories.GenreRepository;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.PopularMediaRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TmdbService {

    private final TmdbClient tmdbClient;

    private final MediaRepository mediaRepository;

    private final PopularMediaRepository  popularMediaRepository;
    
    private final GenreRepository genreRepository;

    @PostConstruct
    public void syncGenres(){
        List<TmdbGenreDTO> movieGenres = tmdbClient.fetchGenres("movie");
        List<TmdbGenreDTO> showGenres = tmdbClient.fetchGenres("tv");

        Map<Long, String> genreMap = new HashMap<>();
        movieGenres.forEach(g -> genreMap.put(g.getId(), g.getName()));
        showGenres.forEach(g -> genreMap.put(g.getId(), g.getName()));

        genreMap.forEach((id, name) -> {
            if (!genreRepository.existsById(id)) {
                genreRepository.save(
                    Genre.builder().id(id).name(name).build()
                );
            }
        });
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
        List<TmdbMovieDTO> results = tmdbClient.fetchPopular(type);

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
        TmdbMovieDTO tmdbMedia = tmdbClient.fetchMediaById(tmdbId, type);

        List<Long> genreIds = tmdbMedia.getGenres() == null
            ? List.of()
            : tmdbMedia.getGenres().stream().map(TmdbGenreDTO::getId).toList();

        List<Genre> genres = genreRepository.findAllById(genreIds);

        return Media.builder()
            .tmdbId(tmdbMedia.getId())
            .title(tmdbMedia.getTitle())
            .overview(tmdbMedia.getOverview())
            .posterPath(tmdbMedia.getPosterPath())
            .releaseDate(TmdbMovieDTO.parseDate(tmdbMedia.getReleaseDate()).orElse(null))
            .rating(tmdbMedia.getVoteAverage())
            .genres(genres)
            .type(type)
            .build();
    }
}
