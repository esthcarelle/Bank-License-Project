package com.bnr.portal.security;

import com.bnr.portal.config.PortalSettings;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtIssuer {

    private final PortalSettings portalSettings;

    public JwtIssuer(PortalSettings portalSettings) {
        this.portalSettings = portalSettings;
    }

    public String issueTokenFor(SignedInUser user) {
        long now = System.currentTimeMillis();
        var tokens = portalSettings.getTokens();
        Date expiry = new Date(now + tokens.getExpirationMs());
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(new Date(now))
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public Long userIdFromToken(String rawJwt) {
        Claims payload = parse(rawJwt);
        Number uid = payload.get("uid", Number.class);
        if (uid != null) {
            return uid.longValue();
        }
        throw new IllegalArgumentException("Invalid token");
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        String secret = portalSettings.getTokens().getSecret();
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes) for HS256");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
