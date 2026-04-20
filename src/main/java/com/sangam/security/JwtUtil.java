package com.sangam.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET        = "HanumanSangamSecretKeyForJWTTokenGenerationMustBe256Bits!";
    private static final long   EXPIRATION_MS = 86_400_000L; // 24 hours

    // FIX: Changed Key → SecretKey (required by JJWT 0.12.x API)
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());

    // ── Generate Token ────────────────────────────────────────────
    public String generateToken(String phoneNumber, String role) {
        return Jwts.builder()
                // FIX: subject() replaces deprecated setSubject()
                .subject(phoneNumber)
                // FIX: claim() stays the same
                .claim("role", role)
                // FIX: issuedAt() replaces deprecated setIssuedAt()
                .issuedAt(new Date())
                // FIX: expiration() replaces deprecated setExpiration()
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                // FIX: signWith(key) — no need to pass SignatureAlgorithm,
                //      JJWT 0.12.x detects it automatically from the key type
                .signWith(key)
                .compact();
    }

    // ── Extract Phone Number ──────────────────────────────────────
    public String extractPhone(String token) {
        return getClaims(token).getSubject();
    }

    // ── Extract Role ──────────────────────────────────────────────
    public String extractRole(String token) {
        return (String) getClaims(token).get("role");
    }

    // ── Validate Token ────────────────────────────────────────────
    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ── Parse Claims ──────────────────────────────────────────────
    private Claims getClaims(String token) {
        // FIX: parserBuilder() + setSigningKey() + parseClaimsJws() + getBody()
        //      are all deprecated/removed in 0.12.x.
        //      New API: parser() + verifyWith() + parseSignedClaims() + getPayload()
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}