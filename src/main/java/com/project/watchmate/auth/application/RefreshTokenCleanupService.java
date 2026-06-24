package com.project.watchmate.auth.application;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.auth.persistence.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupService.class);

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${watchmate.refresh-token.cleanup.revoked-retention-days:7}")
    private int revokedRetentionDays;

    @Scheduled(cron = "${watchmate.refresh-token.cleanup.cron:0 0 3 * * *}")
    @Transactional
    public void deleteExpiredAndOldRevokedTokens() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusDays(revokedRetentionDays);
        int deleted = refreshTokenRepository.deleteExpiredAndOldRevoked(now, cutoff);
        log.info("Refresh token cleanup complete: deleted={} cutoffDays={}", deleted, revokedRetentionDays);
    }
}
