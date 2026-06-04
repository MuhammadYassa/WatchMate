package com.project.watchmate.Services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.Models.Media;
import com.project.watchmate.Models.MediaType;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Models.WatchStatus;
import com.project.watchmate.Repositories.UserMediaStatusRepository;
import com.project.watchmate.Repositories.UserShowTrackingRepository;

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
