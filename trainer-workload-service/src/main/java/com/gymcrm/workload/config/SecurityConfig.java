package com.gymcrm.workload.config;

import com.gymcrm.workload.security.CustomAccessDeniedHandler;
import com.gymcrm.workload.security.CustomAuthenticationEntryPoint;
import com.gymcrm.workload.security.ServiceJwtAuthenticationFilter;
import com.gymcrm.workload.security.ServiceTokenValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

//every /api/ call must carry a valid bearer token (ROLE_SERVICE)
//machine-to-machine API, only one rule
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_INFRASTRUCTURE_PATHS = {
            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs", "/v3/api-docs/**",
            "/actuator/**", "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                     ServiceTokenValidator tokenValidator,
                                                     CustomAuthenticationEntryPoint authenticationEntryPoint,
                                                     CustomAccessDeniedHandler accessDeniedHandler) throws Exception {
        ServiceJwtAuthenticationFilter serviceJwtAuthenticationFilter =
                new ServiceJwtAuthenticationFilter(tokenValidator);

        http
                //stateless bearer token, thus no csrf protection
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_INFRASTRUCTURE_PATHS).permitAll()
                        .requestMatchers("/api/**").hasRole("SERVICE")
                        .anyRequest().authenticated())
                .addFilterBefore(serviceJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
