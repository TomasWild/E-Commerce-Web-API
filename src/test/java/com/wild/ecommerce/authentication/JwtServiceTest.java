package com.wild.ecommerce.authentication;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private UserDetails userDetails;

    // Base64URL encoded 256-bit key for HS256
    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long TEST_JWT_EXPIRATION = 3600000; // 1 hour
    private static final long TEST_REFRESH_EXPIRATION = 86400000; // 24 hours

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", TEST_JWT_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", TEST_REFRESH_EXPIRATION);

        userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    void generateToken_Success() {
        // When
        String token = jwtService.generateToken(userDetails);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void generateToken_WithExtraClaims_Success() {
        // Given
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "USER");
        extraClaims.put("customField", "customValue");

        // When
        String token = jwtService.generateToken(extraClaims, userDetails);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();

        Claims claims = jwtService.extractClaim(token, claims1 -> claims1);

        assertThat(claims.get("role")).isEqualTo("USER");
        assertThat(claims.get("customField")).isEqualTo("customValue");
    }

    @Test
    void generateRefreshToken_Success() {
        // When
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        assertThat(refreshToken.split("\\.")).hasSize(3);
    }

    @Test
    void extractUsername_Success() {
        // Given
        String token = jwtService.generateToken(userDetails);

        // When
        String username = jwtService.extractUsername(token);

        // Then
        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    void extractClaim_ExtractsSubject() {
        // Given
        String token = jwtService.generateToken(userDetails);

        // When
        String subject = jwtService.extractClaim(token, Claims::getSubject);

        // Then
        assertThat(subject).isEqualTo("test@example.com");
    }

    @Test
    void extractClaim_ExtractsIssuedAt() {
        // Given
        String token = jwtService.generateToken(userDetails);

        // When
        Date issuedAt = jwtService.extractClaim(token, Claims::getIssuedAt);

        // Then
        assertThat(issuedAt).isNotNull();
        assertThat(issuedAt).isBeforeOrEqualTo(new java.util.Date());
    }

    @Test
    void extractClaim_ExtractsExpiration() {
        // Given
        String token = jwtService.generateToken(userDetails);

        // When
        Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new java.util.Date());
    }

    @Test
    void isTokenValid_ValidToken_ReturnsTrue() {
        // Given
        String token = jwtService.generateToken(userDetails);

        // When
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValid_WrongUser_ReturnsFalse() {
        // Given
        String token = jwtService.generateToken(userDetails);

        UserDetails differentUser = User.builder()
                .username("different@example.com")
                .password("password")
                .authorities("ROLE_USER")
                .build();

        // When
        boolean isValid = jwtService.isTokenValid(token, differentUser);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValid_ExpiredToken_ReturnsFalse() {
        // Given
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1L); // 1ms
        String token = jwtService.generateToken(userDetails);

        // Wait for the token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When & Then
        assertThatThrownBy(() -> jwtService.isTokenValid(token, userDetails))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parseToken_WithInvalidSignature_ThrowsException() {
        // Given
        String token = jwtService.generateToken(userDetails);

        ReflectionTestUtils.setField(jwtService, "secretKey",
                "504E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");

        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(token))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void parseToken_ExpiredToken_ThrowsExpiredJwtException() {
        // Given
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1L);
        String token = jwtService.generateToken(userDetails);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void generateToken_DifferentTokensForSameUser() {
        // Given
        String token1 = jwtService.generateToken(userDetails);

        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String token2 = jwtService.generateToken(userDetails);

        // Then
        assertThat(token1).isNotEqualTo(token2);

        assertThat(jwtService.isTokenValid(token1, userDetails)).isTrue();
        assertThat(jwtService.isTokenValid(token2, userDetails)).isTrue();
    }

    @Test
    void refreshToken_HasLongerExpiration() {
        // Given
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // When
        Date accessExpiration = jwtService.extractClaim(accessToken, Claims::getExpiration);
        Date refreshExpiration = jwtService.extractClaim(refreshToken, Claims::getExpiration);

        // Then
        assertThat(refreshExpiration).isAfter(accessExpiration);
    }

    @Test
    void extractClaim_WithCustomClaim() {
        // Given
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", 123);
        extraClaims.put("role", "ADMIN");

        String token = jwtService.generateToken(extraClaims, userDetails);

        // When
        Integer userId = jwtService.extractClaim(token, claims -> claims.get("userId", Integer.class));
        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));

        // Then
        assertThat(userId).isEqualTo(123);
        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    void parseToken_WithMalformedToken_ThrowsException() {
        // Given
        String malformedToken = "not.a.valid.jwt.token";

        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(malformedToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    void parseToken_WithNullToken_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(null))
                .isInstanceOf(Exception.class);
    }

    @Test
    void parseToken_WithEmptyToken_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(""))
                .isInstanceOf(Exception.class);
    }
}
