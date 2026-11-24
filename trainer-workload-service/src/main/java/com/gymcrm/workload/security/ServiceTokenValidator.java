package com.gymcrm.workload.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

//verifies the service-to-service JWT gym-crm attaches to every request
//independent of end-user auth
//this service has no user table and no login endpoint — it only ever validates tokens service
@Slf4j
@Component
public class ServiceTokenValidator {

    static final String SCOPE_CLAIM = "scope";
    static final String INTERNAL_SERVICE_SCOPE = "INTERNAL_SERVICE";
    static final String TARGET_SERVICE_CLAIM = "targetService";

    private final SecretKey signingKey;
    private final String expectedTargetService;

    public ServiceTokenValidator(
            @Value("${security.jwt.inter-service.secret}") String secret,
            @Value("${spring.application.name}") String expectedTargetService) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expectedTargetService = expectedTargetService;
    }

    //returns the caller's service id when the token is valid
    //Never throws, mirrors JwtAuthenticationFilter's security entry point reject approach
    public Optional<String> validate(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();

            String scope = claims.get(SCOPE_CLAIM, String.class);
            if (!INTERNAL_SERVICE_SCOPE.equals(scope)) {
                log.debug("Rejected service token: unexpected scope '{}'", scope);
                return Optional.empty();
            }

            String targetService = claims.get(TARGET_SERVICE_CLAIM, String.class);
            if (targetService != null && !targetService.equals(expectedTargetService)) {
                log.debug("Rejected service token: targeted at '{}', not this service", targetService);
                return Optional.empty();
            }

            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected service token: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
