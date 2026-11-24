package com.gymcrm.security;

import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.*;

//security task 7
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtLogoutHandler")
class JwtLogoutHandlerTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private JwtBlacklistService blacklistService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private JwtLogoutHandler handler;

    @BeforeEach
    void setUp() {
        handler = new JwtLogoutHandler(jwtService, blacklistService);
    }

    @Test
    @DisplayName("a valid token gets blacklisted with its real expiration")
    void validToken_getsBlacklisted() {
        when(request.getHeader("Authorization")).thenReturn("Bearer good.token");
        Instant expiry = Instant.now().plusSeconds(3600);
        when(jwtService.extractExpiration("good.token")).thenReturn(expiry);
        when(jwtService.extractUsername("good.token")).thenReturn("alice.nguyen");

        handler.logout(request, response, null);

        verify(blacklistService).blacklist("good.token", expiry);
    }

    @Test
    @DisplayName("no Authorization header is a harmless no-op")
    void noHeader_isNoOp() {
        when(request.getHeader("Authorization")).thenReturn(null);

        handler.logout(request, response, null);

        verifyNoInteractions(blacklistService, jwtService);
    }

    @Test
    @DisplayName("an already-expired/invalid token is a harmless no-op, not an exception escaping to the caller")
    void invalidToken_isNoOpNotAnException() {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad.token");
        when(jwtService.extractExpiration("bad.token")).thenThrow(new MalformedJwtException("malformed"));

        handler.logout(request, response, null);

        verify(blacklistService, never()).blacklist(any(), any());
    }
}
