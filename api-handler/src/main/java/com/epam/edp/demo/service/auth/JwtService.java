package com.epam.edp.demo.service.auth;

import com.epam.edp.demo.entity.auth.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateToken(CustomUserDetails user) {

        return Jwts.builder()
                .subject(user.getId())
                .claim("role", user.getAuthorities())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key)
                .compact();
    }

    public Date extractExpiration(String token) {
        log.debug("Extracting expiration date from token");
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractSubject(String token) {
        log.debug("Extracting username from token");
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        log.debug("Extracted claims from token: {}", claims);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        log.debug("Extracting all claims");

        return Jwts.parser()
                .verifyWith(key)

                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
