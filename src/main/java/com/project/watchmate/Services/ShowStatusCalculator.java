package com.project.watchmate.Services;

import org.springframework.stereotype.Component;

import com.project.watchmate.Models.ShowTrackingState;
import com.project.watchmate.Models.UserShowProgress;
import com.project.watchmate.Models.WatchStatus;

@Component
public class ShowStatusCalculator {

    public WatchStatus calculate(
        UserShowProgress progress,
        int watchedEligibleCount,
        int airedEligibleCount,
        int totalEligibleCount,
        boolean endedShow
    ) {
        if (progress == null) {
            return WatchStatus.NONE;
        }

        if (watchedEligibleCount <= 0) {
            ShowTrackingState trackingState = progress.getTrackingState();
            if (trackingState == ShowTrackingState.TO_WATCH) {
                return WatchStatus.TO_WATCH;
            }
            if (trackingState == ShowTrackingState.WATCHING) {
                return WatchStatus.WATCHING;
            }
            return WatchStatus.NONE;
        }

        if (endedShow && totalEligibleCount > 0 && watchedEligibleCount >= totalEligibleCount) {
            return WatchStatus.WATCHED;
        }

        if (!endedShow && airedEligibleCount > 0 && watchedEligibleCount >= airedEligibleCount) {
            return WatchStatus.UP_TO_DATE;
        }

        return WatchStatus.WATCHING;
    }
}
