package com.project.watchmate.Services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.Clients.TmdbClient;
import com.project.watchmate.Dto.TmdbGenreDTO;
import com.project.watchmate.Dto.TmdbMovieDTO;
import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Models.Genre;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.PopularMedia;
import com.project.watchmate.Repositories.GenreRepository;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.PopularMediaRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TmdbService {

    private final TmdbClient tmdbClient;

    private final MediaRepository mediaRepository;

    private final PopularMediaRepository  popularMediaRepository;
    
    private final GenreRepository genreRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void popularMediaOnStartup() {
        popularMedia();
    }


    @PostConstruct
    public void syncGenres(){
        List<TmdbGenreDTO> movieGenres = tmdbClient.fetchGenres("movie");
        List<TmdbGenreDTO> showGenres = tmdbClient.fetchGenres("tv");

        Map<Long, String> genreMap = new HashMap<>();
        movieGenres.forEach(g -> genreMap.put(g.getId(), g.getName()));
        showGenres.forEach(g -> genreMap.put(g.getId(), g.getName()));

        final int[] createdCount = {0};
        genreMap.forEach((id, name) -> {
            if (!genreRepository.existsById(id)) {
                genreRepository.save(
                    Genre.builder().id(id).name(name).build()
                );
                createdCount[0]++;
            }
        });
        log.info("TMDB genre sync completed totalFetched={} newGenresCreated={}", genreMap.size(), createdCount[0]);
    }

    @Scheduled(cron = "0 59 23 * * *")
    public void popularMedia(){
        log.info("TMDB popular media sync started");
        try {
            fetchAndStorePopularMedia();
            log.info("TMDB popular media sync completed");
        } catch (RuntimeException ex) {
            log.error("TMDB popular media sync failed", ex);
            throw ex;
        }
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
        log.info("Stored popular media type={} itemCount={}", mediaType, popularList.size());
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
        if (tmdbMedia == null) {
            throw new MediaNotFoundException("TMDB media not found for ID: " + tmdbId);
        }

        List<Long> genreIds = tmdbMedia.getGenres() == null
            ? List.of()
            : tmdbMedia.getGenres().stream().map(TmdbGenreDTO::getId).toList();

        List<Genre> genres = genreRepository.findAllById(genreIds);

        Media media = Media.builder()
            .tmdbId(Objects.requireNonNull(tmdbMedia.getId(), "tmdbMedia.id"))
            .title(tmdbMedia.getTitle())
            .overview(tmdbMedia.getOverview())
            .posterPath(tmdbMedia.getPosterPath())
            .releaseDate(TmdbMovieDTO.parseDate(tmdbMedia.getReleaseDate()).orElse(null))
            .rating(tmdbMedia.getVoteAverage())
            .genres(genres)
            .type(type)
            .build();
        log.info("Fetched media details from TMDB tmdbId={} type={} genreCount={}", tmdbId, type, genres.size());
        return media;
    }
}
