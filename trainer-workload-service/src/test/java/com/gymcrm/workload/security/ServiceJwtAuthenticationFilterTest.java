package com.gymcrm.workload.security;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceJwtAuthenticationFilter")
class ServiceJwtAuthenticationFilterTest {

    @Mock
    private ServiceTokenValidator tokenValidator;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private ServiceJwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        //tokenValidator would null thus no @Mock
        filter = new ServiceJwtAuthenticationFilter(tokenValidator);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("a valid bearer token authenticates the request with ROLE_SERVICE")
    void validToken_authenticates() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer good-token");
        when(tokenValidator.validate("good-token")).thenReturn(Optional.of("gym-crm-service"));

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("gym-crm-service", auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SERVICE")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("no Authorization header leaves the SecurityContext empty and still proceeds")
    void noHeader_leavesContextEmpty() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(tokenValidator);
    }

    @Test
    @DisplayName("an invalid token leaves the SecurityContext empty and still proceeds "
            + "(the entry point rejects downstream, not this filter)")
    void invalidToken_leavesContextEmpty() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(tokenValidator.validate("bad-token")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("a non-Bearer Authorization header is ignored")
    void nonBearerHeader_isIgnored() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(tokenValidator);
    }
}
