package com.project.watchmate.Controllers;

import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Repositories.PopularMediaRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequiredArgsConstructor
public class PopularMediaController {

    private final PopularMediaRepository popularMediaRepository;
    
    @GetMapping("/api/popular")
    public List<PopularMediaResponse> getPopularMedia() {
        return popularMediaRepository.findAll().stream()
        .map(pm -> new PopularMediaResponse(
            pm.getPopularityRank(),
            pm.getMedia().getTitle(),
            pm.getMedia().getOverview(),
            pm.getMedia().getPosterPath(),
            pm.getMedia().getRating(),
            pm.getMedia().getType().toString()
        )).collect(Collectors.toList());
    }
    
    record PopularMediaResponse(
        int rank,
        String title,
        String overview,
        String posterPath,
        Double rating,
        String type
    ) {}
}
