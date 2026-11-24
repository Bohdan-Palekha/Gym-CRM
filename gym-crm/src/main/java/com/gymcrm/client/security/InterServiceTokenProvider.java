package com.gymcrm.client.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

//separate from JwtService since this auths application to workload service
//workload service need to check only if call is mady by gym-crm
@Component
public class InterServiceTokenProvider {

    static final String SCOPE_CLAIM = "scope";
    static final String INTERNAL_SERVICE_SCOPE = "INTERNAL_SERVICE";
    static final String TARGET_SERVICE_CLAIM = "targetService";

    private final SecretKey signingKey;
    private final long expirationMs;
    private final String callerServiceId;
    private final String targetServiceId;

    public InterServiceTokenProvider(
            @Value("${security.jwt.inter-service.secret}") String secret,
            @Value("${security.jwt.inter-service.expiration-ms:300000}") long expirationMs,
            @Value("${security.jwt.inter-service.caller-id:gym-crm-service}") String callerServiceId,
            @Value("${trainer-workload.service-id:trainer-workload-service}") String targetServiceId) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.callerServiceId = callerServiceId;
        this.targetServiceId = targetServiceId;
    }

    public String generateToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(callerServiceId)
                .claim(SCOPE_CLAIM, INTERNAL_SERVICE_SCOPE)
                .claim(TARGET_SERVICE_CLAIM, targetServiceId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .id(UUID.randomUUID().toString())
                //explicit algorithm because since we configure key in both services, they can land on different algorithms and fail verification
                //and produce error like "key too weak for HS512"
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }
}
