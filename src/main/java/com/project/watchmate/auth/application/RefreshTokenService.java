package com.project.watchmate.auth.application;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.common.error.InvalidRefreshTokenException;
import com.project.watchmate.auth.domain.RefreshToken;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.auth.persistence.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken createRefreshToken(Users user) {
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = Objects.requireNonNull(RefreshToken.builder()
            .token(UUID.randomUUID().toString())
            .user(user)
            .expiryDate(LocalDateTime.now().plusDays(7))
            .createdAt(LocalDateTime.now())
            .revoked(false)
            .build());

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new InvalidRefreshTokenException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())){
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token)
            .ifPresent(refreshToken -> {
                refreshToken.setRevoked(true);
                refreshTokenRepository.save(refreshToken);
            });
    }

    @Transactional
    public void revokeAllUserTokens(Users user) {
        refreshTokenRepository.deleteByUser(user);
    }
}



