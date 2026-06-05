package com.project.watchmate.media.catalog.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.media.catalog.domain.Media;
import com.project.watchmate.media.catalog.domain.MediaType;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.media.catalog.domain.WatchStatus;
import com.project.watchmate.movie.tracking.persistence.UserMediaStatusRepository;
import com.project.watchmate.show.tracking.persistence.UserShowTrackingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserWatchStatusResolver {

    private final UserMediaStatusRepository userMediaStatusRepository;

    private final UserShowTrackingRepository userShowTrackingRepository;

    @Transactional(readOnly = true)
    public WatchStatus resolveWatchStatus(Users user, Media media) {
        if (user == null || media == null) {
            return WatchStatus.NONE;
        }

        if (media.getType() == MediaType.SHOW) {
            return userShowTrackingRepository.findByUserAndMedia(user, media)
                .map(tracking -> tracking.getStatus())
                .orElse(WatchStatus.NONE);
        }

        return userMediaStatusRepository.findByUserAndMedia(user, media)
            .map(status -> status.getStatus())
            .orElse(WatchStatus.NONE);
    }
}





