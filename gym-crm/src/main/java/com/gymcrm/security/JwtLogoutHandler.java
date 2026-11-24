package com.gymcrm.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

//security task 7
//logout is configured by spring security's .logout() in SecurityConfig
//spring security's logout filter runs before jwt authentication filter
//thus auth here is generally null and invalid/no token is harmless
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtLogoutHandler implements LogoutHandler {

    private final JwtService jwtService;
    private final JwtBlacklistService blacklistService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String token = BearerTokenExtractor.extractToken(request);
        if (token == null) {
            return;
        }
        try {
            blacklistService.blacklist(token, jwtService.extractExpiration(token));
            log.info("Logged out username={}", jwtService.extractUsername(token));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Logout called with an already invalid/expired bearer token: {}", e.getMessage());
        }
    }
}
