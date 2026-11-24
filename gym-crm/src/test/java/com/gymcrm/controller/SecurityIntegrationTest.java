package com.gymcrm.controller;

import com.gymcrm.dao.UserDao;
import com.gymcrm.dto.request.LoginRequest;
import com.gymcrm.dto.request.TraineeRegistrationRequest;
import com.gymcrm.dto.request.TrainerRegistrationRequest;
import com.gymcrm.dto.response.LoginResponse;
import com.gymcrm.dto.response.RegistrationResponse;
import com.gymcrm.model.TrainingType;
import com.gymcrm.service.TraineeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;



//unlike other controller test this one has  filter chain enabled
//authentication is required everywhere besides exceptions, salted hashing, login, brute-force
//lockout, logout, JWT bearer tokens, and CORS
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:gymcrm_security_it;DB_CLOSE_DELAY=-1;MODE=MySQL",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.hikari.maximum-pool-size=3",
                "hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "hibernate.hbm2ddl.auto=create-drop",
                "hibernate.show_sql=false",
                "hibernate.format_sql=false"
        })
@DisplayName("Spring Security integration (security tasks 2-9)")
class SecurityIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TraineeService traineeService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private RegistrationResponse registerTrainee(String firstName, String lastName) {
        TraineeRegistrationRequest request = new TraineeRegistrationRequest(firstName, lastName, null, null);
        ResponseEntity<RegistrationResponse> response =
                restTemplate.postForEntity("/api/trainees", request, RegistrationResponse.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody();
    }

    @Nested
    @DisplayName("task 2 — authentication required everywhere except the documented exceptions")
    class PublicVsProtected {

        @Test
        @DisplayName("a protected endpoint with no Authorization header returns 401")
        void protectedEndpoint_noToken_returns401() {
            ResponseEntity<String> response =
                    restTemplate.getForEntity("/api/trainees/no-such-user", String.class);
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("a protected endpoint with a garbage token returns 401, not 500")
        void protectedEndpoint_garbageToken_returns401NotError() {
            HttpEntity<Void> entity = new HttpEntity<>(bearer("not-a-real-jwt"));
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/trainees/no-such-user", HttpMethod.GET, entity, String.class);
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("task 2's one exception — POST /api/trainees needs no token")
        void createTraineeProfile_isPublic() {
            RegistrationResponse body = registerTrainee("Pub", "LicOne");
            assertNotNull(body.token(), "security task 8: registration itself returns a usable bearer token");
        }

        @Test
        @DisplayName("task 2's other exception — POST /api/trainers needs no token")
        void createTrainerProfile_isPublic() {
            TrainerRegistrationRequest request =
                    new TrainerRegistrationRequest("Pub", "LicTrainer", TrainingType.TrainingTypeName.YOGA);
            ResponseEntity<RegistrationResponse> response =
                    restTemplate.postForEntity("/api/trainers", request, RegistrationResponse.class);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody().token());
        }

        @Test
        @DisplayName("GET /api/training-types requires authentication, same as any other business endpoint")
        void trainingTypes_requiresAuthentication() {
            ResponseEntity<String> withoutToken = restTemplate.getForEntity("/api/training-types", String.class);
            assertEquals(HttpStatus.UNAUTHORIZED, withoutToken.getStatusCode());

            RegistrationResponse registered = registerTrainee("Types", "NeedAuth");
            HttpEntity<Void> entity = new HttpEntity<>(bearer(registered.token()));
            ResponseEntity<String> withToken = restTemplate.exchange(
                    "/api/training-types", HttpMethod.GET, entity, String.class);
            assertEquals(HttpStatus.OK, withToken.getStatusCode());
        }

        @Test
        @DisplayName("security task 8 — the token returned at registration works on a protected endpoint")
        void registrationToken_worksOnProtectedEndpoint() {
            RegistrationResponse registered = registerTrainee("Token", "Works");

            HttpEntity<Void> entity = new HttpEntity<>(bearer(registered.token()));
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/trainees/" + registered.username(), HttpMethod.GET, entity, String.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("security tasks 3, 5 — Login")
    class Login {

        @Test
        @DisplayName("valid credentials return 200 with a token that works on a protected endpoint")
        void validCredentials_returnUsableToken() {
            RegistrationResponse registered = registerTrainee("Login", "Valid");

            LoginRequest loginRequest = new LoginRequest(registered.username(), registered.password());
            ResponseEntity<LoginResponse> loginResponse =
                    restTemplate.postForEntity("/api/auth/login", loginRequest, LoginResponse.class);

            assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
            assertEquals(registered.username(), loginResponse.getBody().username());

            HttpEntity<Void> entity = new HttpEntity<>(bearer(loginResponse.getBody().token()));
            ResponseEntity<String> profile = restTemplate.exchange(
                    "/api/trainees/" + registered.username(), HttpMethod.GET, entity, String.class);
            assertEquals(HttpStatus.OK, profile.getStatusCode());
        }

        @Test
        @DisplayName("wrong password returns 401")
        void wrongPassword_returns401() {
            RegistrationResponse registered = registerTrainee("Login", "Wrong");

            LoginRequest loginRequest = new LoginRequest(registered.username(), "definitely-not-it");
            ResponseEntity<String> response =
                    restTemplate.postForEntity("/api/auth/login", loginRequest, String.class);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("unknown username returns 401, same as a wrong password would (no user-enumeration signal)")
        void unknownUsername_returns401() {
            LoginRequest loginRequest = new LoginRequest("no.such.user.at.all", "whatever");
            ResponseEntity<String> response =
                    restTemplate.postForEntity("/api/auth/login", loginRequest, String.class);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("security task 6 — Brute force protector")
    class BruteForceProtection {

        @Test
        @DisplayName("3 consecutive failures lock the account; even the correct password is then rejected with 423")
        void thirdFailureLocksAccount_evenCorrectPasswordThenRejected() {
            RegistrationResponse registered = registerTrainee("Brute", "Force");

            LoginRequest wrongCredentials = new LoginRequest(registered.username(), "wrong-password");
            for (int attempt = 1; attempt <= 3; attempt++) {
                ResponseEntity<String> response =
                        restTemplate.postForEntity("/api/auth/login", wrongCredentials, String.class);
                assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),
                        "attempt " + attempt + " should still be a plain bad-credentials rejection");
            }

            LoginRequest correctCredentials = new LoginRequest(registered.username(), registered.password());
            ResponseEntity<String> lockedResponse =
                    restTemplate.postForEntity("/api/auth/login", correctCredentials, String.class);

            assertEquals(HttpStatus.LOCKED, lockedResponse.getStatusCode(),
                    "locked out for 5 minutes regardless of whether the password is now correct");
        }

        @Test
        @DisplayName("fewer than 3 failures do not lock the account")
        void belowThreshold_doesNotLock() {
            RegistrationResponse registered = registerTrainee("Brute", "Under");

            LoginRequest wrongCredentials = new LoginRequest(registered.username(), "wrong-password");
            restTemplate.postForEntity("/api/auth/login", wrongCredentials, String.class);
            restTemplate.postForEntity("/api/auth/login", wrongCredentials, String.class);

            LoginRequest correctCredentials = new LoginRequest(registered.username(), registered.password());
            ResponseEntity<LoginResponse> response =
                    restTemplate.postForEntity("/api/auth/login", correctCredentials, LoginResponse.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("security task 7 — Logout")
    class Logout {

        @Test
        @DisplayName("a token works, then is rejected for every subsequent request right after logout")
        void logout_invalidatesTheToken() {
            RegistrationResponse registered = registerTrainee("Logout", "Flow");
            HttpEntity<Void> authed = new HttpEntity<>(bearer(registered.token()));

            ResponseEntity<String> beforeLogout = restTemplate.exchange(
                    "/api/trainees/" + registered.username(), HttpMethod.GET, authed, String.class);
            assertEquals(HttpStatus.OK, beforeLogout.getStatusCode());

            ResponseEntity<String> logoutResponse = restTemplate.exchange(
                    "/api/auth/logout", HttpMethod.POST, authed, String.class);
            assertEquals(HttpStatus.OK, logoutResponse.getStatusCode());

            ResponseEntity<String> afterLogout = restTemplate.exchange(
                    "/api/trainees/" + registered.username(), HttpMethod.GET, authed, String.class);
            assertEquals(HttpStatus.UNAUTHORIZED, afterLogout.getStatusCode(),
                    "same token, already logged out, must no longer be accepted");
        }

        @Test
        @DisplayName("logging out with no token at all is a harmless no-op, not an error")
        void logout_withNoToken_isHarmless() {
            ResponseEntity<String> response =
                    restTemplate.postForEntity("/api/auth/logout", null, String.class);
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("security task 4 — salted password hashing")
    class PasswordHashing {

        @Test
        @DisplayName("the password stored in the DB is a salted hash, never the plaintext returned to the client")
        @org.springframework.transaction.annotation.Transactional
        void storedPassword_isHashedNotPlaintext() {
            RegistrationResponse registered = registerTrainee("Hash", "Check");

            String storedPassword = userDao.findByUsername(registered.username()).orElseThrow().getPassword();

            assertNotEquals(registered.password(), storedPassword);
            assertTrue(passwordEncoder.matches(registered.password(), storedPassword));
        }
    }

    @Nested
    @DisplayName("criterion 5.3 — deactivation and security")
    class Deactivation {

        @Test
        @DisplayName("a deactivated account can no longer log in")
        void deactivatedAccount_cannotLogIn() {
            RegistrationResponse registered = registerTrainee("Deactivated", "CannotLogin");
            traineeService.activateDeactivate(registered.username(), false);

            LoginRequest loginRequest = new LoginRequest(registered.username(), registered.password());
            ResponseEntity<String> response =
                    restTemplate.postForEntity("/api/auth/login", loginRequest, String.class);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("an already-issued token stops working the moment the account is deactivated")
        void existingToken_stopsWorkingImmediatelyOnDeactivation() {
            RegistrationResponse registered = registerTrainee("Deactivated", "TokenDies");
            HttpEntity<Void> authed = new HttpEntity<>(bearer(registered.token()));

            ResponseEntity<String> beforeDeactivation = restTemplate.exchange(
                    "/api/trainees/" + registered.username(), HttpMethod.GET, authed, String.class);
            assertEquals(HttpStatus.OK, beforeDeactivation.getStatusCode());

            traineeService.activateDeactivate(registered.username(), false);

            ResponseEntity<String> afterDeactivation = restTemplate.exchange(
                    "/api/trainees/" + registered.username(), HttpMethod.GET, authed, String.class);
            assertEquals(HttpStatus.UNAUTHORIZED, afterDeactivation.getStatusCode(),
                    "JwtAuthenticationFilter reloads UserDetails fresh on every request");
        }
    }

    @Nested
    @DisplayName("security task 9 — CORS")
    class Cors {

        @Test
        @DisplayName("a cross-origin request gets Access-Control-Allow-Origin reflecting the caller's origin, "
                + "even on a 401 (CORS headers are set ahead of the authentication check in the chain)")
        void crossOriginRequest_getsAllowOriginHeader() {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ORIGIN, "https://example.com");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/trainees/anyone", HttpMethod.GET, entity, String.class);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), "no token was sent");
            assertEquals("https://example.com",
                    response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }

        @Test
        @DisplayName("a preflight OPTIONS request is answered without requiring authentication")
        void preflightRequest_isAnsweredWithoutAuthentication() {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ORIGIN, "https://example.com");
            headers.set(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/trainees/anyone", HttpMethod.OPTIONS, entity, String.class);

            assertTrue(response.getStatusCode().is2xxSuccessful());
        }
    }
}
