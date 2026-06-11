package com.project.watchmate.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.common.error.InvalidRefreshTokenException;
import com.project.watchmate.common.error.UnauthorizedRefreshTokenAccessException;
import com.project.watchmate.auth.domain.RefreshToken;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.auth.persistence.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public IssuedRefreshToken createRefreshToken(Users user) {
        IssuedRefreshToken issuedRefreshToken = buildRefreshToken(user);
        RefreshToken savedRefreshToken = refreshTokenRepository.save(issuedRefreshToken.refreshToken());
        return new IssuedRefreshToken(savedRefreshToken, issuedRefreshToken.rawToken());
    }

    @Transactional
    public IssuedRefreshToken rotateRefreshToken(String token) {
        RefreshToken presentedToken = loadRefreshTokenForUpdate(token);
        validateRefreshToken(presentedToken);

        presentedToken.setRevoked(true);
        refreshTokenRepository.save(presentedToken);

        IssuedRefreshToken replacementRefreshToken = buildRefreshToken(presentedToken.getUser());
        RefreshToken savedReplacement = refreshTokenRepository.save(replacementRefreshToken.refreshToken());
        return new IssuedRefreshToken(savedReplacement, replacementRefreshToken.rawToken());
    }

    @Transactional
    public void revokeRefreshTokenForUser(String token, Users authenticatedUser) {
        RefreshToken refreshToken = loadRefreshTokenForUpdate(token);
        validateRefreshToken(refreshToken);

        if (!refreshToken.getUser().getId().equals(authenticatedUser.getId())) {
            throw new UnauthorizedRefreshTokenAccessException("Refresh token does not belong to the authenticated user");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    private RefreshToken loadRefreshTokenForUpdate(String token) {
        return refreshTokenRepository.findByTokenHashForUpdate(hashToken(token))
            .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));
    }

    private void validateRefreshToken(RefreshToken refreshToken) {
        if (refreshToken.isRevoked()) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())){
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }
    }

    private IssuedRefreshToken buildRefreshToken(Users user) {
        String rawToken = generateRawToken();
        RefreshToken refreshToken = Objects.requireNonNull(RefreshToken.builder()
            .tokenHash(hashToken(rawToken))
            .user(user)
            .expiryDate(LocalDateTime.now().plusDays(7))
            .createdAt(LocalDateTime.now())
            .revoked(false)
            .build());
        return new IssuedRefreshToken(refreshToken, rawToken);
    }

    private String generateRawToken() {
        byte[] tokenBytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(Objects.requireNonNull(token, "token").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public record IssuedRefreshToken(RefreshToken refreshToken, String rawToken) {
    }
}



