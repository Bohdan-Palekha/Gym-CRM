package com.gymcrm.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

//place to pull raw token from bearer, shared by jwt auth filter, jwt logout handler
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
