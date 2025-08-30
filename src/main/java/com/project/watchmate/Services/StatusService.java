package com.project.watchmate.Services;

import org.springframework.stereotype.Service;

import com.project.watchmate.Dto.UpdateWatchStatusRequestDTO;
import com.project.watchmate.Dto.UserMediaStatusDTO;
import com.project.watchmate.Exception.InvalidWatchStatusException;
import com.project.watchmate.Exception.MediaNotFoundException;
import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.UserMediaStatus;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.MediaRepository;
import com.project.watchmate.Repositories.UserMediaStatusRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatusService {

	private final MediaRepository mediaRepository;

	private final UserMediaStatusRepository userMediaStatusRepository;

	public UserMediaStatusDTO updateWatchStatus(Users user, UpdateWatchStatusRequestDTO request) {
		Media media = mediaRepository.findByTmdbId(request.getTmdbId())
				.orElseThrow(() -> new MediaNotFoundException("Media does not exist."));

		WatchStatus desiredStatus = parseWatchStatus(request.getStatus());

		UserMediaStatus userMediaStatus = userMediaStatusRepository.findByUserAndMedia(user, media)
				.orElse(UserMediaStatus.builder()
						.user(user)
						.media(media)
						.status(WatchStatus.NONE)
						.build());

		userMediaStatus.setStatus(desiredStatus);
		userMediaStatusRepository.save(userMediaStatus);

		return UserMediaStatusDTO.builder()
				.tmdbId(media.getTmdbId())
				.status(userMediaStatus.getStatus())
				.build();
	}

	private WatchStatus parseWatchStatus(String statusString) {
		if (statusString == null) {
			throw new InvalidWatchStatusException("Status must be provided.");
		}
		String normalized = statusString.trim().toUpperCase();
		switch (normalized) {
			case "TO_WATCH":
				return WatchStatus.TO_wATCH;
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


