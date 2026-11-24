package com.gymcrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymcrm.dto.request.ChangeLoginRequest;
import com.gymcrm.dto.request.LoginRequest;
import com.gymcrm.security.JwtService;
import com.gymcrm.service.AuthenticationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//filters disabled here to test AuthController in isolation
//actual security behavior is in SecurityIntegrationTest
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtService jwtService;

    private static Principal authenticatedAs(String username) {
        return new UsernamePasswordAuthenticationToken(username, null);
    }

    @Nested
    @DisplayName("security tasks 3,5,8 — Login")
    class Login {

        @Test
        @DisplayName("valid credentials return 200 with a bearer token")
        void validCredentials_returnsOkWithToken() throws Exception {
            Authentication authentication = new UsernamePasswordAuthenticationToken("alice.nguyen", null);
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(jwtService.generateToken("alice.nguyen")).thenReturn("mocked.jwt.token");

            LoginRequest request = new LoginRequest("alice.nguyen", "pw");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("alice.nguyen"))
                    .andExpect(jsonPath("$.token").value("mocked.jwt.token"));
        }

        @Test
        @DisplayName("bad credentials (unknown username or wrong password alike) return 401")
        void badCredentials_returnsUnauthorized() throws Exception {
            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            LoginRequest request = new LoginRequest("alice.nguyen", "wrong");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("security task 6 — locked-out username returns 423")
        void lockedOut_returnsLocked() throws Exception {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new LockedException("Account locked"));

            LoginRequest request = new LoginRequest("alice.nguyen", "pw");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is(423));
        }

        @Test
        @DisplayName("criterion 5.3 — deactivated account returns 403")
        void deactivatedAccount_returnsForbidden() throws Exception {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new DisabledException("Account disabled"));

            LoginRequest request = new LoginRequest("alice.nguyen", "pw");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("blank username returns 400")
        void blankUsername_returnsBadRequest() throws Exception {
            LoginRequest request = new LoginRequest(" ", "pw");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(authenticationManager);
        }

        @Test
        @DisplayName("missing password field returns 400")
        void missingPasswordField_returnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"alice.nguyen\"}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(authenticationManager);
        }
    }

    @Nested
    @DisplayName("REST task 4 — Change Login")
    class ChangeLogin {

        @Test
        @DisplayName("changes the authenticated caller's own password and returns 200")
        void validRequest_changesAuthenticatedCallersPassword() throws Exception {
            ChangeLoginRequest request = new ChangeLoginRequest("old", "newPass123");

            mockMvc.perform(put("/api/auth/change-password")
                            .principal(authenticatedAs("alice.nguyen"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(authenticationService).changePassword("alice.nguyen", "old", "newPass123");
        }

        @Test
        @DisplayName("ChangeLoginRequest has no username field to target another account with")
        void requestBodyHasNoUsernameField() {
            for (var component : ChangeLoginRequest.class.getRecordComponents()) {
                if ("username".equals(component.getName())) {
                    throw new AssertionError("ChangeLoginRequest must not have a username field");
                }
            }
        }

        @Test
        @DisplayName("blank new password returns 400")
        void blankNewPassword_returnsBadRequest() throws Exception {
            ChangeLoginRequest request = new ChangeLoginRequest("old", " ");

            mockMvc.perform(put("/api/auth/change-password")
                            .principal(authenticatedAs("alice.nguyen"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authenticationService);
        }

        @Test
        @DisplayName("wrong old password returns 401")
        void wrongOldPassword_returnsUnauthorized() throws Exception {
            doThrow(new SecurityException("bad password"))
                    .when(authenticationService).changePassword("alice.nguyen", "wrong", "newPass123");

            ChangeLoginRequest request = new ChangeLoginRequest("wrong", "newPass123");

            mockMvc.perform(put("/api/auth/change-password")
                            .principal(authenticatedAs("alice.nguyen"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
