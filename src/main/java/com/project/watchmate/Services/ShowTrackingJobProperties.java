package com.project.watchmate.Services;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "watchmate.show-jobs")
public class ShowTrackingJobProperties {

    private boolean enabled = true;

    private long pollDelayMs = 5000L;

    private int maxJobsPerPoll = 3;

    private int staleRunningMinutes = 15;

    private int maxAttempts = 3;
}
