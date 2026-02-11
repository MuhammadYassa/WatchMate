package com.project.watchmate.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String TEST_SECRET_BASE64 = "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=";

    @Mock
    private UserDetails userDetails;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET_BASE64);
    }

    @Nested
    @DisplayName("Generate Access Token Tests")
    class GenerateAccessTokenTests {

        @Test
        void generateAccessToken_WithUsername_ReturnsNonEmptyToken() {
            String token = jwtService.generateAccessToken("testuser");

            assertNotNull(token);
            assertFalse(token.isBlank());
        }

        @Test
        void generateAccessToken_WithUsername_ExtractedUsernameMatches() {
            String username = "jane.doe";
            String token = jwtService.generateAccessToken(username);

            String extracted = jwtService.extractUsername(token);
            assertEquals(username, extracted);
        }
    }

    @Nested
    @DisplayName("Generate Token Tests")
    class GenerateTokenTests {

        @Test
        void generateToken_WithUsername_ReturnsSameAsGenerateAccessToken() {
            String username = "alice";
            String accessToken = jwtService.generateAccessToken(username);
            String token = jwtService.generateToken(username);

            assertNotNull(token);
            assertEquals(jwtService.extractUsername(accessToken), jwtService.extractUsername(token));
        }
    }

    @Nested
    @DisplayName("Get Access Token Expiry Tests")
    class GetAccessTokenExpiryTests {

        @Test
        void getAccessTokenExpiry_ReturnsApproximatelyNowPlus15Minutes() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            LocalDateTime expiry = jwtService.getAccessTokenExpiry();
            LocalDateTime after = LocalDateTime.now().plusMinutes(15).plusSeconds(2);

            assertTrue(!expiry.isBefore(before.plusMinutes(15)));
            assertTrue(!expiry.isAfter(after));
        }
    }

    @Nested
    @DisplayName("Extract Username Tests")
    class ExtractUsernameTests {

        @Test
        void extractUsername_WithValidToken_ReturnsUsername() {
            String username = "bob";
            String token = jwtService.generateAccessToken(username);

            assertEquals(username, jwtService.extractUsername(token));
        }

        @Test
        void extractUsername_WithInvalidToken_ThrowsException() {
            String invalidToken = "invalid.jwt.token";

            assertThrows(Exception.class, () -> jwtService.extractUsername(invalidToken));
        }
    }

    @Nested
    @DisplayName("Validate Token Tests")
    class ValidateTokenTests {

        @Test
        void validateToken_WithValidTokenAndMatchingUser_ReturnsTrue() {
            String username = "validuser";
            when(userDetails.getUsername()).thenReturn(username);
            String token = jwtService.generateAccessToken(username);

            assertTrue(jwtService.validateToken(token, userDetails));
        }

        @Test
        void validateToken_WithValidTokenAndWrongUser_ReturnsFalse() {
            String token = jwtService.generateAccessToken("tokenuser");
            when(userDetails.getUsername()).thenReturn("differentuser");

            assertFalse(jwtService.validateToken(token, userDetails));
        }

        @Test
        void validateToken_WithExpiredToken_ThrowsExpiredJwtException() {
            String username = "expireduser";
            String expiredToken = buildExpiredToken(username);

            assertThrows(ExpiredJwtException.class,
                    () -> jwtService.validateToken(expiredToken, userDetails));
        }

        @Test
        void validateToken_WithInvalidToken_ThrowsException() {
            assertThrows(Exception.class,
                    () -> jwtService.validateToken("not.a.valid.jwt", userDetails));
        }

        private String buildExpiredToken(String username) {
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET_BASE64));
            return Jwts.builder()
                    .subject(username)
                    .issuedAt(new Date(System.currentTimeMillis() - 20000))
                    .expiration(new Date(System.currentTimeMillis() - 10000))
                    .signWith(key)
                    .compact();
        }
    }
}
