package com.epam.edp.demo.service.auth;

import com.epam.edp.demo.entity.auth.CustomUserDetails;
import com.epam.edp.demo.entity.auth.Role;
import com.epam.edp.demo.entity.auth.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
    private static final String OTHER_SECRET =
            Base64.getEncoder().encodeToString("FEDCBA9876543210FEDCBA9876543210".getBytes(StandardCharsets.UTF_8));

    private JwtService service;

    @BeforeEach
    void setUp() {
        service = new JwtService(SECRET);
    }

    @Test
    void generateToken_andExtractUsername_roundTripsSubject() {
        User user = new User(
                "user-123",
                "foo@bar.com",
                "pw",
                "John",
                "Doe",
                Role.CUSTOMER
        );
        CustomUserDetails userDetails = new CustomUserDetails(user);

        String token = service.generateToken(userDetails);

        assertThat(service.extractSubject(token)).isEqualTo("user-123");
    }

    @Test
    void generateToken_setsExpirationApproximately24HoursFromNow() {
        User user = new User(
                "user-123",
                "foo@bar.com",
                "pw",
                "John",
                "Doe",
                Role.CUSTOMER
        );
        CustomUserDetails userDetails = new CustomUserDetails(user);

        long expectedExpiration = System.currentTimeMillis() + 86_400_000L;

        String token = service.generateToken(userDetails);

        Date expiration = service.extractExpiration(token);
        long delta = expiration.getTime() - expectedExpiration;
        assertThat(Math.abs(delta)).isLessThanOrEqualTo(60_000L);
    }

    @Test
    void extractUsername_malformedToken_throwsJwtException() {
        assertThatThrownBy(() -> service.extractSubject("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractExpiration_malformedToken_throwsJwtException() {
        assertThatThrownBy(() -> service.extractExpiration("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractUsername_tokenSignedWithDifferentSecret_throwsJwtException() {
        JwtService otherService = new JwtService(OTHER_SECRET);
        User user = new User(
                "user-123",
                "foo@bar.com",
                "pw",
                "John",
                "Doe",
                Role.CUSTOMER
        );
        CustomUserDetails userDetails = new CustomUserDetails(user);

        String tokenFromOther = otherService.generateToken(userDetails);

        assertThatThrownBy(() -> service.extractSubject(tokenFromOther))
                .isInstanceOf(JwtException.class);
    }
}
