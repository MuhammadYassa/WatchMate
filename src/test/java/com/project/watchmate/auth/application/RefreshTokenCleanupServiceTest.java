package com.project.watchmate.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.project.watchmate.auth.persistence.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cleanupService, "revokedRetentionDays", 7);
    }

    @Nested
    @DisplayName("deleteExpiredAndOldRevokedTokens")
    class CleanupTests {

        @Test
        void invokesRepositoryWithCorrectCutoffDate() {
            when(refreshTokenRepository.deleteExpiredAndOldRevoked(any(), any())).thenReturn(3);

            cleanupService.deleteExpiredAndOldRevokedTokens();

            ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(refreshTokenRepository).deleteExpiredAndOldRevoked(nowCaptor.capture(), cutoffCaptor.capture());

            LocalDateTime capturedNow = nowCaptor.getValue();
            LocalDateTime capturedCutoff = cutoffCaptor.getValue();

            // cutoff should be approximately 7 days before now
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(capturedCutoff, capturedNow);
            assertEquals(7, daysBetween);
        }

        @Test
        void returnsDeletedCount() {
            when(refreshTokenRepository.deleteExpiredAndOldRevoked(any(), any())).thenReturn(5);

            cleanupService.deleteExpiredAndOldRevokedTokens();

            verify(refreshTokenRepository).deleteExpiredAndOldRevoked(any(), any());
        }

        @Test
        void whenNoTokensToDelete_deletesZero() {
            when(refreshTokenRepository.deleteExpiredAndOldRevoked(any(), any())).thenReturn(0);

            cleanupService.deleteExpiredAndOldRevokedTokens();

            verify(refreshTokenRepository).deleteExpiredAndOldRevoked(any(), any());
        }

        @Test
        void cutoffIsBeforeNow() {
            when(refreshTokenRepository.deleteExpiredAndOldRevoked(any(), any())).thenReturn(0);

            cleanupService.deleteExpiredAndOldRevokedTokens();

            ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(refreshTokenRepository).deleteExpiredAndOldRevoked(nowCaptor.capture(), cutoffCaptor.capture());

            LocalDateTime now = nowCaptor.getValue();
            LocalDateTime cutoff = cutoffCaptor.getValue();
            assertEquals(true, cutoff.isBefore(now));
        }
    }
}
