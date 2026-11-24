package com.gymcrm.workload.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

//never reject here, leave SecurityContext empty on failure same as gym-crm
//SecurityContext into a 401 for any endpoint that actually requires ROLE_SERVICE
//no @Component, constructed in SecurityConfig to avoid picked as a generic servlet and register 2nd time
@Slf4j
public class ServiceJwtAuthenticationFilter extends OncePerRequestFilter {

    static final String ROLE_SERVICE = "ROLE_SERVICE";

    private final ServiceTokenValidator tokenValidator;

    public ServiceJwtAuthenticationFilter(ServiceTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String token = BearerTokenExtractor.extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            tokenValidator.validate(token).ifPresentOrElse(
                    callerServiceId -> SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    callerServiceId, null, List.of(new SimpleGrantedAuthority(ROLE_SERVICE)))),
                    () -> log.debug("Rejected bearer token on {} {}", request.getMethod(), request.getRequestURI()));
        }
        filterChain.doFilter(request, response);
    }
}
