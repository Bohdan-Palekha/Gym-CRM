package com.gymcrm.config;

import com.gymcrm.security.CustomAccessDeniedHandler;
import com.gymcrm.security.CustomAuthenticationEntryPoint;
import com.gymcrm.security.JwtAuthenticationFilter;
import com.gymcrm.security.JwtBlacklistService;
import com.gymcrm.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

//security tasks 2-9
//http basic/form login are not enabled,  auth is bearer JWT,
//validated per-request by JwtAuthenticationFilter(added into filet chain)
//stateless sessions, a JWT carries everything needed, so no HttpSession
//authorizeHttpRequests is the source for which endpoints are public
//logout is registered through http security .logout() with a custom LogoutHandler
//CORS is configured, wired in via .cors()
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_INFRASTRUCTURE_PATHS = {
            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs", "/v3/api-docs/**",
            "/actuator/**", "/error"
    };

    //bcrypt salts every hash
    //password encoder prefixes stored hash with {bcrypt}
    //thus it's possible to migrate to another encryption without password invalidation
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    //exposes auth manager so AuthController can call auth manager .authenticate() for POST /api/auth/login
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${security.cors.allowed-origins:*}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        //pattern form so a "*" default remains compatible with allowCredentials:
        //spring reflects request origin
        configuration.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Authorization", "X-Transaction-Id", "Location"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                     CorsConfigurationSource corsConfigurationSource,
                                                     JwtService jwtService,
                                                     UserDetailsService userDetailsService,
                                                     JwtBlacklistService jwtBlacklistService,
                                                     CustomAuthenticationEntryPoint authenticationEntryPoint,
                                                     CustomAccessDeniedHandler accessDeniedHandler,
                                                     LogoutHandler jwtLogoutHandler) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtService, userDetailsService, jwtBlacklistService);

        http
                //stateless bearer-token API: no cookies/HttpSession in play, so nothing for
                //CSRF to protect
                //stateless bearer token api, no cookies, no HttpSession, nothing for csrf to do
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        //everything except create Trainer/Trainee profile"
                        .requestMatchers(HttpMethod.POST, "/api/trainees", "/api/trainers").permitAll()
                        //can't auth to reach a login endpoint that itself requires auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        //API docs and infrastructure endpoints ,not business
                        .requestMatchers(PUBLIC_INFRASTRUCTURE_PATHS).permitAll()
                        //everything else, requires a valid bearer token
                        .anyRequest().authenticated())
                //task 7
                //spring security logout mechanism
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .addLogoutHandler(jwtLogoutHandler)
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_OK)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
