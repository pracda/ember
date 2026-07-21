package com.ember.security;

import com.ember.config.EmberProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/** Issues and verifies the staff JWTs. HMAC-SHA256 over the configured secret. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlMillis;

    public JwtService(EmberProperties props) {
        this.key = Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = props.getJwt().getTtl().toMillis();
    }

    /** Mint a token for a signed-in staff member, carrying their single role claim. */
    public String generate(String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttlMillis)))
                .signWith(key)
                .compact();
    }

    /** Verify signature + expiry and return the claims; throws {@code JwtException} if invalid. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
