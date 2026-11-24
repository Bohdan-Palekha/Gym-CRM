package com.gymcrm.controller;

import com.gymcrm.annotation.Sensitive;
import com.gymcrm.dto.request.ChangeLoginRequest;
import com.gymcrm.dto.request.LoginRequest;
import com.gymcrm.dto.response.ErrorResponse;
import com.gymcrm.dto.response.LoginResponse;
import com.gymcrm.security.JwtService;
import com.gymcrm.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

//password change is guarded by a bearer token, the caller is identified from the authenticated principal
//cannot change another user's password and name
//hands out JWT
//brute-force + cred check is in authenticationManager.authenticate
//logout is handled by Spring Security in SecurityConfig

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, logout and password management")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Operation(summary = "Log in (security tasks 3, 5, 8)",
            description = "Public endpoint. Verifies username/password - subject to the brute-force "
                    + "lockout after 3 consecutive failures (task 6) - and returns a JWT bearer token.",
            security = {})
    @ApiResponse(responseCode = "200", description = "Login succeeded, token returned")
    @ApiResponse(responseCode = "400", description = "Username or password missing/blank",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Invalid username or password",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "423", description = "Account temporarily locked (too many failed attempts)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Account is deactivated",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/login")
    @Sensitive
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        String token = jwtService.generateToken(authentication.getName());
        return ResponseEntity.ok(new LoginResponse(authentication.getName(), token));
    }

    @Operation(summary = "Log out (security task 7)",
            description = "Invalidates the caller's current bearer token so it can no longer be "
                    + "used, even though it hasn't expired yet. Handled entirely by Spring "
                    + "Security's own logout mechanism (see SecurityConfig / JwtLogoutHandler); "
                    + "this method exists only so the endpoint is discoverable in the generated "
                    + "API documentation. Spring Security's LogoutFilter intercepts and answers "
                    + "the request before it would ever reach this controller.")
    @ApiResponse(responseCode = "200", description = "Logged out (also 200 if no/an already-invalid token was sent)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Void> logoutDocumentationOnly() {
        throw new IllegalStateException(
                "Unreachable: Spring Security's LogoutFilter must handle POST /api/auth/logout "
                        + "before it reaches DispatcherServlet - check the SecurityConfig filter chain.");
    }

    @Operation(summary = "Change the authenticated user's password (REST task 4)",
            description = "Guarded by a Bearer token; changes the caller's own password only — "
                    + "there is no way to target another account. The old password is re-checked.")
    @ApiResponse(responseCode = "200", description = "Password changed")
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Invalid old password",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/change-password")
    @Sensitive
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangeLoginRequest request) {
        authenticationService.changePassword(authentication.getName(), request.oldPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}
