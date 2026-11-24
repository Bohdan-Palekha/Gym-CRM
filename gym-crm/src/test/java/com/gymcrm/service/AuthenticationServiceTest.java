package com.gymcrm.service;

import com.gymcrm.dao.UserDao;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.metrics.GymMetrics;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService")
class AuthenticationServiceTest {

    @Mock
    private UserDao userDao;
    @Mock
    private UsernameService usernameService;
    @Mock
    private GymMetrics gymMetrics;

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private AuthenticationService service;
    private Trainee sampleTrainee;
    private Trainer sampleTrainer;

    @BeforeEach
    void setUp() {
        service = new AuthenticationService();
        service.setUserDao(userDao);
        service.setUsernameService(usernameService);
        service.setGymMetrics(gymMetrics);
        service.setPasswordEncoder(passwordEncoder);

        sampleTrainee = new Trainee();
        sampleTrainee.setUsername("Alice.Smith");
        sampleTrainee.setPassword(passwordEncoder.encode("secretPass1"));
        sampleTrainee.setActive(true);

        sampleTrainer = new Trainer();
        sampleTrainer.setUsername("Bob.Jones");
        sampleTrainer.setPassword(passwordEncoder.encode("trainerPass1"));
        sampleTrainer.setActive(true);
    }

    @Nested
    @DisplayName("authenticate")
    class Authenticate {

        @Test
        @DisplayName("correct trainee credentials pass silently and record a trainee success")
        void correctTraineeCredentials_passes() {
            when(userDao.findByUsername("Alice.Smith")).thenReturn(Optional.of(sampleTrainee));

            assertDoesNotThrow(() -> service.authenticate("Alice.Smith", "secretPass1"));

            verify(gymMetrics).recordTraineeAuthSuccess();
            verifyNoMoreInteractions(gymMetrics);
        }

        @Test
        @DisplayName("correct trainer credentials pass silently and record a trainer success")
        void correctTrainerCredentials_passes() {
            when(userDao.findByUsername("Bob.Jones")).thenReturn(Optional.of(sampleTrainer));

            assertDoesNotThrow(() -> service.authenticate("Bob.Jones", "trainerPass1"));

            verify(gymMetrics).recordTrainerAuthSuccess();
            verifyNoMoreInteractions(gymMetrics);
        }

        @Test
        @DisplayName("wrong password throws SecurityException and records a role-tagged failure")
        void wrongPassword_throwsAndRecordsFailure() {
            when(userDao.findByUsername("Alice.Smith")).thenReturn(Optional.of(sampleTrainee));

            assertThrows(SecurityException.class,
                    () -> service.authenticate("Alice.Smith", "wrongPass"));

            verify(gymMetrics).recordTraineeAuthFailure();
            verify(gymMetrics, never()).recordTraineeAuthSuccess();
        }

        @Test
        @DisplayName("unknown username throws EntityNotFoundException and records a role-agnostic failure")
        void unknownUsername_throwsAndRecordsUnknownFailure() {
            when(userDao.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> service.authenticate("ghost", "any"));

            verify(gymMetrics).recordUnknownUserAuthFailure();
            verify(gymMetrics, never()).recordTraineeAuthFailure();
            verify(gymMetrics, never()).recordTrainerAuthFailure();
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("security task 4 — valid old password persists a *hashed* new password, never plaintext")
        void validOldPassword_traineeBacked_updatesWithHash() {
            when(userDao.findByUsername("Alice.Smith")).thenReturn(Optional.of(sampleTrainee));

            service.changePassword("Alice.Smith", "secretPass1", "newPass999");

            assertNotEquals("newPass999", sampleTrainee.getPassword(), "password must not be stored as plaintext");
            assertTrue(passwordEncoder.matches("newPass999", sampleTrainee.getPassword()));
            verify(userDao).update(sampleTrainee);
            verify(usernameService).validateName("newPass999", "newPassword");
        }

        @Test
        @DisplayName("valid old password updates and persists a trainer-backed User via UserDao")
        void validOldPassword_trainerBacked_updatesWithHash() {
            when(userDao.findByUsername("Bob.Jones")).thenReturn(Optional.of(sampleTrainer));

            service.changePassword("Bob.Jones", "trainerPass1", "newTrainerPass");

            assertTrue(passwordEncoder.matches("newTrainerPass", sampleTrainer.getPassword()));
            verify(userDao).update(sampleTrainer);
        }

        @Test
        @DisplayName("wrong old password throws and does not persist")
        void wrongOldPassword_doesNotPersist() {
            when(userDao.findByUsername("Alice.Smith")).thenReturn(Optional.of(sampleTrainee));

            assertThrows(SecurityException.class,
                    () -> service.changePassword("Alice.Smith", "badOld", "newPass"));

            verify(userDao, never()).update(any());
        }

        @Test
        @DisplayName("unknown username throws and does not persist")
        void unknownUsername_doesNotPersist() {
            when(userDao.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> service.changePassword("ghost", "any", "newPass"));

            verify(userDao, never()).update(any());
        }

        @Test
        @DisplayName("blank new password is rejected by UsernameService before any credential check")
        void blankNewPassword_rejectedUpfront() {
            doThrow(new IllegalArgumentException("newPassword must not be blank"))
                    .when(usernameService).validateName(" ", "newPassword");

            assertThrows(IllegalArgumentException.class,
                    () -> service.changePassword("Alice.Smith", "secretPass1", " "));

            verifyNoInteractions(userDao);
        }
    }
}
