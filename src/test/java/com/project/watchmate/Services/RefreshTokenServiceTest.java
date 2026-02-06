package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.watchmate.Models.RefreshToken;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Repositories.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Nested
    @DisplayName("Create Refresh Token Tests")
    class CreateRefreshTokenTests{
        @Test
        void createRefreshToken_WithValidCredentials_ShouldSaveAndReturnRefreshToken(){
            Users user = Users.builder().username("testuser").build();

            when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, RefreshToken.class));

            LocalDateTime beforeCall = LocalDateTime.now();

            RefreshToken token = refreshTokenService.createRefreshToken(user);
            LocalDateTime afterCall = LocalDateTime.now();

            assertNotNull(token);
            assertNotNull(token.getToken());
            assertFalse(token.isRevoked());
            assertEquals(user, token.getUser());

            assertTrue(!token.getExpiryDate().isBefore(beforeCall.plusDays(7)));
            assertTrue(!token.getExpiryDate().isAfter(afterCall.plusDays(7)));

            verify(refreshTokenRepository).deleteByUser(user);

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            RefreshToken saved = captor.getValue();
            assertNotNull(saved);

            assertEquals(user, saved.getUser());
            assertNotNull(saved.getToken());
            assertFalse(saved.isRevoked());
            assertTrue(!saved.getExpiryDate().isBefore(beforeCall.plusDays(7)));
            assertTrue(!saved.getExpiryDate().isAfter(afterCall.plusDays(7)));
            }
    }
}
