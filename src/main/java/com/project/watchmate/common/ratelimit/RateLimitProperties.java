package com.project.watchmate.common.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "watchmate.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    private int loginCapacity = 5;
    private long loginRefillSeconds = 60;

    private int registerCapacity = 3;
    private long registerRefillSeconds = 60;

    private int resendCapacity = 2;
    private long resendRefillSeconds = 60;

    private int refreshCapacity = 10;
    private long refreshRefillSeconds = 60;

    /** How long a bucket entry lives without any access before being evicted (seconds). */
    private long bucketTtlSeconds = 3600;

    /** How often the stale-bucket cleanup runs (seconds). */
    private long bucketCleanupIntervalSeconds = 300;
}
