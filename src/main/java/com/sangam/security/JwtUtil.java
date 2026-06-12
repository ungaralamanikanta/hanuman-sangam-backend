package com.sangam.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${JWT_SECRET}")
    private String jwtSecret;

    private SecretKey key;

    private static final long EXPIRATION_MS =
            7_200_000L; // 2 Hours

    @PostConstruct
    public void init() {

        this.key = Keys.hmacShaKeyFor(
                jwtSecret.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }

    public String generateToken(
            String phoneNumber,
            String role) {

        return Jwts.builder()

                .issuer("Hanuman-Sangam")

                .subject(phoneNumber)

                .claim("role", role)

                .issuedAt(new Date())

                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + EXPIRATION_MS
                        )
                )

                .signWith(key)

                .compact();
    }

    public String extractPhone(
            String token) {

        return getClaims(token)
                .getSubject();
    }

    public String extractRole(
            String token) {

        return (String)
                getClaims(token)
                        .get("role");
    }

    public boolean isTokenValid(
            String token) {

        try {

            Claims claims =
                    getClaims(token);

            return claims
                    .getExpiration()
                    .after(new Date());

        } catch (Exception e) {

            return false;
        }
    }

    private Claims getClaims(
            String token) {

        return Jwts.parser()

                .verifyWith(key)

                .build()

                .parseSignedClaims(token)

                .getPayload();
    }
}
