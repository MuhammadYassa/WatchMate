package com.project.watchmate.Services;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.project.watchmate.Clients.TmdbClient;
import com.project.watchmate.Dto.DiscoveryMediaItemDTO;
import com.project.watchmate.Dto.GenreBrowseResponseDTO;
import com.project.watchmate.Dto.TmdbResponseDTO;
import com.project.watchmate.Exception.GenreNotFoundException;
import com.project.watchmate.Mappers.WatchMateMapper;
import com.project.watchmate.Models.GenreLookup;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Repositories.GenreLookupRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GenreBrowseService {

    private final GenreLookupRepository genreLookupRepository;

    private final TmdbClient tmdbClient;

    private final WatchMateMapper watchMateMapper;

    public GenreBrowseResponseDTO browseMovies(String genreName, int page, int size) {
        return browse(genreName, MediaType.MOVIE, "movie", page, size);
    }

    public GenreBrowseResponseDTO browseShows(String genreName, int page, int size) {
        return browse(genreName, MediaType.SHOW, "tv", page, size);
    }

    private GenreBrowseResponseDTO browse(String genreName, MediaType mediaType, String tmdbType, int page, int size) {
        GenreLookup genreLookup = genreLookupRepository.findByNameIgnoreCaseAndMediaType(Objects.requireNonNull(genreName, "genreName"), mediaType)
            .orElseThrow(() -> new GenreNotFoundException("Genre not found: " + genreName));

        TmdbResponseDTO response = tmdbClient.discoverByGenre(tmdbType, genreLookup.getTmdbGenreId(), page);
        List<DiscoveryMediaItemDTO> results = response == null || response.getResults() == null
            ? List.of()
            : response.getResults().stream()
                .limit(size)
                .map(result -> watchMateMapper.mapToDiscoveryMediaItemDTO(result, mediaType))
                .toList();

        return GenreBrowseResponseDTO.builder()
            .genre(genreLookup.getName())
            .mediaType(mediaType)
            .results(results)
            .currentPage(response == null ? page : response.getPage())
            .totalPages(response == null ? 0 : response.getTotalPages())
            .totalResults(response == null ? 0 : response.getTotalResults())
            .build();
    }
}
