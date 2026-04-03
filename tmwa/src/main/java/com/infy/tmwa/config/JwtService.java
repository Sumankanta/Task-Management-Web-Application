package com.infy.tmwa.config;

import com.infy.tmwa.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

@Service
public class JwtService {

    private final String SECRET = "iajvjdfbiadfav1jdiaue4oancsdjcaweiwew0tiwet";
    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    public String generateToken(User user) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())          // jti claim — unique per token
                .subject(user.getEmail())
                .claim("userId",   user.getId())
                .claim("fullName", user.getFullName())
                .claim("role",     user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86_400_000)) // 24 h
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** Extract the JWT ID (jti) — used for session revocation */
    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    /** Extract expiry as LocalDateTime — stored in TokenBlocklist for housekeeping */
    public LocalDateTime extractExpiry(String token) {
        Date exp = parseClaims(token).getExpiration();
        return Instant.ofEpochMilli(exp.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public Claims extractAllClaims(String token) {
        return parseClaims(token);
    }

    // ── internal ──────────────────────────────────────────────────────
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}