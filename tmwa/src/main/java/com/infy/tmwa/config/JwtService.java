package com.infy.tmwa.config;

import com.infy.tmwa.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;

import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

@Service
public class JwtService {

    private final String SECRET = "iajvjdfbiadfav1jdiaue4oancsdjcaweiwew0tiwet";
    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    public String generateToken(User user){
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("fullName", user.getFullName())
                .claim("role", user.getRole().name())  // "ADMIN" | "MANAGER" | "MEMBER" | "VIEWER"
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key)
                .compact();
    }


    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
    
    // Helper used by AdminController to extract role claim directly if needed
    public Claims extractAllClaims(String token){
        return Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
