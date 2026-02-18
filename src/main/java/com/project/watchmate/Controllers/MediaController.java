package com.project.watchmate.Controllers;

import org.springframework.web.bind.annotation.RestController;

import com.project.watchmate.Dto.MediaDetailsDTO;
import com.project.watchmate.Dto.PaginatedSearchResponseDTO;
import com.project.watchmate.Dto.ReviewResponseDTO;
import com.project.watchmate.Dto.UpdateWatchStatusRequestDTO;
import com.project.watchmate.Dto.UserMediaStatusDTO;
import com.project.watchmate.Models.UserPrincipal;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Repositories.PopularMediaRepository;
import com.project.watchmate.Services.MediaService;
import com.project.watchmate.Services.ReviewService;
import com.project.watchmate.Services.SearchService;
import com.project.watchmate.Services.StatusService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Validated
public class MediaController {

    private final MediaService mediaService;

    private final SearchService searchService;

    private final PopularMediaRepository popularMediaRepository;

    private final StatusService statusService;

    private final ReviewService reviewService;

    record PopularMediaResponse(
        int rank,
        String title,
        String overview,
        String posterPath,
        Double rating,
        String type
    ) {}

    @GetMapping("/{tmdbId}")
    public ResponseEntity<MediaDetailsDTO> getMediaDetails(
        @PathVariable @Min(1) Long tmdbId,
        @RequestParam("type") @NotBlank String typeStr,
        @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Users user = userPrincipal.getUser();
        MediaDetailsDTO dto = mediaService.getMediaDetails(tmdbId, typeStr, user);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/search")
    public ResponseEntity<PaginatedSearchResponseDTO> getSearchResponse(
        @RequestParam("query") @NotBlank String query,
        @RequestParam(value = "page", defaultValue = "1") @Min(1) int page
    ) {
        PaginatedSearchResponseDTO dto = searchService.search(query, page);
        return ResponseEntity.ok(dto);
    }
    
    @GetMapping("/popular")
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

    @PostMapping("/update")
	public ResponseEntity<UserMediaStatusDTO> updateStatus(
			@AuthenticationPrincipal UserPrincipal userPrincipal,
			@Valid @RequestBody UpdateWatchStatusRequestDTO request) {
		Users user = userPrincipal.getUser();
		UserMediaStatusDTO dto = statusService.updateWatchStatus(user, request);
		return ResponseEntity.ok(dto);
	}

    @GetMapping("/{mediaId}/reviews")
    public ResponseEntity<List<ReviewResponseDTO>> getReviews(@AuthenticationPrincipal UserPrincipal userPrincipal, @PathVariable @Min(1) Long mediaId) {
        Users user = userPrincipal.getUser();
        List<ReviewResponseDTO> reviewResponses = reviewService.getReviews(user, mediaId);
        return ResponseEntity.ok(reviewResponses);
    }
}
