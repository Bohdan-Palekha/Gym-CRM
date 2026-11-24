package com.gymcrm.workload.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

//mirrors gym-crm's security.BearerTokenExtractor
final class BearerTokenExtractor {

    private static final String BEARER_PREFIX = "Bearer ";

    private BearerTokenExtractor() {
    }

    static String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
