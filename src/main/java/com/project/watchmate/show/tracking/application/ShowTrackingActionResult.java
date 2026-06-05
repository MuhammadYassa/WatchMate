package com.project.watchmate.show.tracking.application;

import com.project.watchmate.show.jobs.dto.ShowTrackingJobDTO;

public record ShowTrackingActionResult<T>(T completedBody, ShowTrackingJobDTO acceptedJob) {

    public static <T> ShowTrackingActionResult<T> completed(T body) {
        return new ShowTrackingActionResult<>(body, null);
    }

    public static <T> ShowTrackingActionResult<T> accepted(ShowTrackingJobDTO job) {
        return new ShowTrackingActionResult<>(null, job);
    }

    public boolean isAccepted() {
        return acceptedJob != null;
    }
}


