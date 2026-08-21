package com.tsix.immocrm.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Signs and parses the JWT carried in the {@code access_token} httpOnly cookie (see
 * {@code AuthController}). Stateless: every claim needed to authorize a request is embedded in
 * the token itself — {@code tokenVersion} is the one exception, checked against
 * {@code AppUserCredential.tokenVersion} on each request once the enforcing filter lands, so a
 * password change/admin reset can invalidate previously issued tokens without a blacklist table.
 */
@Service
public class JwtService {

    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_EMPLOYEE_ID = "employeeId";
    public static final String CLAIM_MUST_CHANGE_PASSWORD = "mustChangePassword";
    public static final String CLAIM_PROFILE_COMPLETED = "profileCompleted";
    public static final String CLAIM_TOKEN_VERSION = "tokenVersion";
    public static final String CLAIM_USERNAME = "username";

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generate(UUID admUserId, String username, List<String> roles, UUID employeeId,
                            boolean mustChangePassword, boolean profileCompleted, int tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(admUserId.toString())
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_EMPLOYEE_ID, employeeId != null ? employeeId.toString() : null)
                .claim(CLAIM_MUST_CHANGE_PASSWORD, mustChangePassword)
                .claim(CLAIM_PROFILE_COMPLETED, profileCompleted)
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /** @throws io.jsonwebtoken.JwtException if the token is malformed, expired, or has an invalid signature. */
    public Claims parse(String token) {
        Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        return jws.getPayload();
    }

    public long expirationSeconds() {
        return expirationMinutes * 60;
    }
}
