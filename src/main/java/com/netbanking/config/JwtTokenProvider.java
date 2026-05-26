package com.netbanking.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.security.SecureRandom;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    private static final int MIN_KEY_BYTES = 32;

    @Value("${app.jwt.secret-hex:}")
    private String jwtSecretHex;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationInMs;

    private Key signingKey;

    @PostConstruct
    void initializeSigningKey() {
        byte[] keyBytes;
        if (jwtSecretHex == null || jwtSecretHex.isBlank()) {
            keyBytes = new byte[MIN_KEY_BYTES];
            new SecureRandom().nextBytes(keyBytes);
            log.warn("JWT_SECRET_HEX is not configured. Generated an in-memory development signing key.");
        } else {
            keyBytes = decodeHex(jwtSecretHex.trim());
        }

        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("JWT_SECRET_HEX must decode to at least 32 bytes.");
        }

        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private Key getSigningKey() {
        return signingKey;
    }

    private byte[] decodeHex(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalStateException("JWT_SECRET_HEX must contain an even number of hex characters.");
        }

        byte[] data = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalStateException("JWT_SECRET_HEX contains non-hex characters.");
            }
            data[i / 2] = (byte) ((high << 4) + low);
        }
        return data;
    }

    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromJwt(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public String getRoleFromJwt(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("role", String.class);
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty.");
        }
        return false;
    }
}
