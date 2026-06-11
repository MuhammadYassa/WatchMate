package com.project.watchmate.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.auth.domain.RefreshToken;
import com.project.watchmate.auth.persistence.RefreshTokenRepository;
import com.project.watchmate.common.error.InvalidRefreshTokenException;
import com.project.watchmate.common.error.UnauthorizedRefreshTokenAccessException;
import com.project.watchmate.user.domain.Users;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Nested
    @DisplayName("Create Refresh Token Tests")
    class CreateRefreshTokenTests {
        @Test
        void createRefreshToken_WithValidCredentials_ShouldSaveAndReturnRefreshToken() {
            Users user = Users.builder().username("testuser").build();

            when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, RefreshToken.class));

            LocalDateTime beforeCall = LocalDateTime.now();

            RefreshTokenService.IssuedRefreshToken issuedToken = refreshTokenService.createRefreshToken(user);
            LocalDateTime afterCall = LocalDateTime.now();

            assertNotNull(issuedToken);
            assertNotNull(issuedToken.rawToken());
            assertFalse(issuedToken.rawToken().isBlank());
            assertNotEquals(issuedToken.rawToken(), issuedToken.refreshToken().getTokenHash());
            assertEquals(RefreshTokenService.hashToken(issuedToken.rawToken()), issuedToken.refreshToken().getTokenHash());
            assertFalse(issuedToken.refreshToken().isRevoked());
            assertEquals(user, issuedToken.refreshToken().getUser());
            assertTrue(!issuedToken.refreshToken().getExpiryDate().isBefore(beforeCall.plusDays(7)));
            assertTrue(!issuedToken.refreshToken().getExpiryDate().isAfter(afterCall.plusDays(7)));

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            RefreshToken saved = captor.getValue();
            assertNotNull(saved);
            assertEquals(user, saved.getUser());
            assertNotNull(saved.getTokenHash());
            assertEquals(RefreshTokenService.hashToken(issuedToken.rawToken()), saved.getTokenHash());
            assertNotEquals(issuedToken.rawToken(), saved.getTokenHash());
            assertFalse(saved.isRevoked());
        }
    }

    @Nested
    @DisplayName("Rotate Refresh Token Tests")
    class RotateRefreshTokenTests {
        @Test
        void rotateRefreshToken_WithValidToken_ShouldRevokePresentedTokenAndReturnReplacement() {
            Users user = Users.builder().id(1L).username("testuser").build();
            String rawPresentedToken = "refresh-token-old";
            RefreshToken existingToken = RefreshToken.builder()
                .tokenHash(RefreshTokenService.hashToken(rawPresentedToken))
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now().minusDays(1))
                .revoked(false)
                .build();

            when(refreshTokenRepository.findByTokenHashForUpdate(RefreshTokenService.hashToken(rawPresentedToken))).thenReturn(Optional.of(existingToken));
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, RefreshToken.class));

            RefreshTokenService.IssuedRefreshToken replacement = refreshTokenService.rotateRefreshToken(rawPresentedToken);

            assertNotNull(replacement);
            assertEquals(user, replacement.refreshToken().getUser());
            assertFalse(replacement.refreshToken().isRevoked());
            assertNotNull(replacement.rawToken());
            assertNotEquals(replacement.rawToken(), replacement.refreshToken().getTokenHash());
            assertEquals(RefreshTokenService.hashToken(replacement.rawToken()), replacement.refreshToken().getTokenHash());
            assertTrue(existingToken.isRevoked());
            assertNotEquals(existingToken.getTokenHash(), replacement.refreshToken().getTokenHash());
        }

        @Test
        void rotateRefreshToken_WithInvalidToken_ShouldThrowInvalidRefreshTokenException() {
            when(refreshTokenRepository.findByTokenHashForUpdate(RefreshTokenService.hashToken("bad-token"))).thenReturn(Optional.empty());

            InvalidRefreshTokenException exception = assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotateRefreshToken("bad-token")
            );

            assertEquals("Invalid refresh token", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Revoke Owned Refresh Token Tests")
    class RevokeOwnedRefreshTokenTests {
        @Test
        void revokeRefreshTokenForUser_WithOwnedToken_ShouldRevokeRefreshToken() {
            Users user = Users.builder().id(1L).username("owner").build();
            String rawToken = "refresh-token-to-be-revoked";
            RefreshToken someRefreshToken = RefreshToken.builder()
                .tokenHash(RefreshTokenService.hashToken(rawToken))
                .user(user)
                .revoked(false)
                .expiryDate(LocalDateTime.now().plusDays(1))
                .build();
            when(refreshTokenRepository.findByTokenHashForUpdate(RefreshTokenService.hashToken(rawToken)))
                .thenReturn(Optional.of(someRefreshToken));

            refreshTokenService.revokeRefreshTokenForUser(rawToken, user);

            ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
            RefreshToken savedToken = refreshTokenCaptor.getValue();
            assertNotNull(savedToken);
            assertTrue(savedToken.isRevoked());
        }

        @Test
        void revokeRefreshTokenForUser_WithTokenOwnedByAnotherUser_ShouldThrowForbidden() {
            Users authenticatedUser = Users.builder().id(1L).username("owner").build();
            Users otherUser = Users.builder().id(2L).username("other").build();
            String rawToken = "refresh-token-to-be-revoked";
            RefreshToken someRefreshToken = RefreshToken.builder()
                .tokenHash(RefreshTokenService.hashToken(rawToken))
                .user(otherUser)
                .revoked(false)
                .expiryDate(LocalDateTime.now().plusDays(1))
                .build();
            when(refreshTokenRepository.findByTokenHashForUpdate(RefreshTokenService.hashToken(rawToken)))
                .thenReturn(Optional.of(someRefreshToken));

            UnauthorizedRefreshTokenAccessException exception = assertThrows(
                UnauthorizedRefreshTokenAccessException.class,
                () -> refreshTokenService.revokeRefreshTokenForUser(rawToken, authenticatedUser)
            );

            assertEquals("Refresh token does not belong to the authenticated user", exception.getMessage());
            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }
    }
}
