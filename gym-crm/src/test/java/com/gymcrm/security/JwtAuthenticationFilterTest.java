package com.gymcrm.security;

import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

//security task 8
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtBlacklistService blacklistService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, blacklistService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserDetails enabledUser(String username) {
        return User.withUsername(username).password("$2a$hashed").authorities("ROLE_TRAINEE").build();
    }

    @Test
    @DisplayName("no Authorization header: continues the chain, leaves SecurityContext empty")
    void noAuthorizationHeader_continuesUnauthenticated() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("header without the 'Bearer ' prefix is ignored")
    void nonBearerHeader_ignored() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("valid token for an enabled user authenticates the request")
    void validToken_authenticates() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer good.token");
        when(blacklistService.isBlacklisted("good.token")).thenReturn(false);
        when(jwtService.extractUsername("good.token")).thenReturn("alice.nguyen");
        UserDetails userDetails = enabledUser("alice.nguyen");
        when(userDetailsService.loadUserByUsername("alice.nguyen")).thenReturn(userDetails);
        when(jwtService.isTokenValid("good.token", userDetails)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("alice.nguyen", authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TRAINEE")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("security task 7 — a blacklisted (logged-out) token is rejected")
    void blacklistedToken_notAuthenticated() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer logged.out.token");
        when(blacklistService.isBlacklisted("logged.out.token")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("criterion 5.3 — a token for a now-deactivated user is rejected on this very request")
    void deactivatedUser_notAuthenticated() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer good.token");
        when(blacklistService.isBlacklisted("good.token")).thenReturn(false);
        when(jwtService.extractUsername("good.token")).thenReturn("alice.nguyen");
        UserDetails disabledUser = User.withUsername("alice.nguyen").password("x")
                .authorities("ROLE_TRAINEE").disabled(true).build();
        when(userDetailsService.loadUserByUsername("alice.nguyen")).thenReturn(disabledUser);
        when(jwtService.isTokenValid("good.token", disabledUser)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("an expired/invalid token is rejected")
    void invalidToken_notAuthenticated() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad.token");
        when(blacklistService.isBlacklisted("bad.token")).thenReturn(false);
        when(jwtService.extractUsername("bad.token")).thenReturn("alice.nguyen");
        UserDetails userDetails = enabledUser("alice.nguyen");
        when(userDetailsService.loadUserByUsername("alice.nguyen")).thenReturn(userDetails);
        when(jwtService.isTokenValid("bad.token", userDetails)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("a well-formed token for a since-deleted user is rejected, not a 500")
    void unknownUser_notAuthenticated() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer good.token");
        when(blacklistService.isBlacklisted("good.token")).thenReturn(false);
        when(jwtService.extractUsername("good.token")).thenReturn("ghost");
        when(userDetailsService.loadUserByUsername("ghost")).thenThrow(new UsernameNotFoundException("ghost"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("a garbage token string is rejected, not a 500")
    void garbageToken_notAuthenticated() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer not-a-real-jwt");
        when(blacklistService.isBlacklisted("not-a-real-jwt")).thenReturn(false);
        when(jwtService.extractUsername("not-a-real-jwt")).thenThrow(new MalformedJwtException("malformed"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("does not re-authenticate if the SecurityContext is already populated")
    void alreadyAuthenticated_skipsReprocessing() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer good.token");
        Authentication existing = new UsernamePasswordAuthenticationToken("someone.else", null);
        SecurityContextHolder.getContext().setAuthentication(existing);

        filter.doFilter(request, response, filterChain);

        assertSame(existing, SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService, blacklistService);
    }
}
