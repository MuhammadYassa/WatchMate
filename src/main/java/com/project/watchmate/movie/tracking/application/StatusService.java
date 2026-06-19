package com.project.watchmate.movie.tracking.application;

import com.project.watchmate.media.catalog.application.MediaResolutionService;

import org.springframework.stereotype.Service;

import com.project.watchmate.common.cache.WatchMateCacheEvictionService;
import com.project.watchmate.common.dto.UpdateWatchStatusRequestDTO;
import com.project.watchmate.movie.tracking.dto.UserMediaStatusDTO;
import com.project.watchmate.common.error.InvalidWatchStatusException;
import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.movie.tracking.domain.UserMediaStatus;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.movie.tracking.persistence.UserMediaStatusRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatusService {

	private final MediaResolutionService mediaResolutionService;

	private final UserMediaStatusRepository userMediaStatusRepository;

	private final WatchMateCacheEvictionService cacheEvictionService;

	public UserMediaStatusDTO updateWatchStatus(Users user, Long tmdbId, MediaType mediaType, UpdateWatchStatusRequestDTO request) {
		if (mediaType != MediaType.MOVIE) {
			throw new IllegalArgumentException("StatusService only handles movie watch status updates.");
		}

		WatchStatus desiredStatus = parseMovieWatchStatus(request.getStatus());
		Media media = mediaResolutionService.resolveMediaByTmdbId(tmdbId, mediaType);

		if (desiredStatus == WatchStatus.NONE) {
			userMediaStatusRepository.findByUserAndMedia(user, media)
				.ifPresent(userMediaStatusRepository::delete);
			cacheEvictionService.evictUserProgressCaches(user.getId());

			return UserMediaStatusDTO.builder()
				.tmdbId(media.getTmdbId())
				.status(WatchStatus.NONE)
				.build();
		}

		UserMediaStatus userMediaStatus = userMediaStatusRepository.findByUserAndMedia(user, media)
				.orElse(UserMediaStatus.builder()
						.user(user)
						.media(media)
						.status(desiredStatus)
						.build());

		userMediaStatus.setStatus(desiredStatus);
		userMediaStatusRepository.save(userMediaStatus);
		cacheEvictionService.evictUserProgressCaches(user.getId());

		return UserMediaStatusDTO.builder()
				.tmdbId(media.getTmdbId())
				.status(userMediaStatus.getStatus())
				.build();
	}

	private WatchStatus parseMovieWatchStatus(String statusString) {
		if (statusString == null) {
			throw new InvalidWatchStatusException("Status must be provided.");
		}
		String normalized = statusString.trim().toUpperCase();
		switch (normalized) {
			case "TO_WATCH":
				return WatchStatus.TO_WATCH;
			case "WATCHING":
				return WatchStatus.WATCHING;
			case "WATCHED":
				return WatchStatus.WATCHED;
			case "NONE":
				return WatchStatus.NONE;
			default:
				throw new InvalidWatchStatusException("Invalid status. Allowed: TO_WATCH, WATCHING, WATCHED, NONE");
		}
	}
}








