package com.gymcrm.security;

import com.gymcrm.dao.UserDao;
import com.gymcrm.metrics.GymMetrics;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureDisabledEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationEventListener")
class AuthenticationEventListenerTest {

    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private GymMetrics gymMetrics;
    @Mock
    private UserDao userDao;

    private AuthenticationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new AuthenticationEventListener(loginAttemptService, gymMetrics, userDao);
    }

    @Test
    @DisplayName("success clears the brute-force counter and records a trainee success metric")
    void success_traineePrincipal_recordsTraineeSuccess() {
        Trainee trainee = new Trainee();
        trainee.setUsername("alice.nguyen");
        UserPrincipal principal = new UserPrincipal(trainee, true);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        listener.onSuccess(new AuthenticationSuccessEvent(authentication));

        verify(loginAttemptService).loginSucceeded("alice.nguyen");
        verify(gymMetrics).recordTraineeAuthSuccess();
        verifyNoInteractions(userDao);
    }

    @Test
    @DisplayName("success with a trainer principal records a trainer success metric")
    void success_trainerPrincipal_recordsTrainerSuccess() {
        Trainer trainer = new Trainer();
        trainer.setUsername("marcus.lee");
        UserPrincipal principal = new UserPrincipal(trainer, true);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        listener.onSuccess(new AuthenticationSuccessEvent(authentication));

        verify(loginAttemptService).loginSucceeded("marcus.lee");
        verify(gymMetrics).recordTrainerAuthSuccess();
    }

    @Test
    @DisplayName("bad credentials for a known trainee increments the brute-force counter and records a trainee failure")
    void badCredentials_knownTrainee_countsAndRecordsFailure() {
        Authentication attempted = new UsernamePasswordAuthenticationToken("alice.nguyen", "wrong");
        Trainee trainee = new Trainee();
        trainee.setUsername("alice.nguyen");
        when(userDao.findByUsername("alice.nguyen")).thenReturn(Optional.of(trainee));

        listener.onBadCredentials(new AuthenticationFailureBadCredentialsEvent(
                attempted, new BadCredentialsException("Bad credentials")));

        verify(loginAttemptService).loginFailed("alice.nguyen");
        verify(gymMetrics).recordTraineeAuthFailure();
        verify(gymMetrics, never()).recordUnknownUserAuthFailure();
    }

    @Test
    @DisplayName("bad credentials for an unknown username still counts toward the lockout and records a role-agnostic failure")
    void badCredentials_unknownUsername_countsAndRecordsUnknownFailure() {
        Authentication attempted = new UsernamePasswordAuthenticationToken("ghost", "whatever");
        when(userDao.findByUsername("ghost")).thenReturn(Optional.empty());

        listener.onBadCredentials(new AuthenticationFailureBadCredentialsEvent(
                attempted, new BadCredentialsException("Bad credentials")));

        verify(loginAttemptService).loginFailed("ghost");
        verify(gymMetrics).recordUnknownUserAuthFailure();
    }

    @Test
    @DisplayName("security task 6 — an attempt rejected for being already-locked does NOT itself count as a further strike")
    void locked_doesNotCountTowardLockout() {
        Authentication attempted = new UsernamePasswordAuthenticationToken("alice.nguyen", "whatever");

        listener.onLocked(new AuthenticationFailureLockedEvent(attempted, new LockedException("locked")));

        verifyNoInteractions(loginAttemptService);
        verifyNoInteractions(gymMetrics);
    }

    @Test
    @DisplayName("an attempt against a deactivated account does not count toward the lockout")
    void disabled_doesNotCountTowardLockout() {
        Authentication attempted = new UsernamePasswordAuthenticationToken("alice.nguyen", "whatever");

        listener.onDisabled(new AuthenticationFailureDisabledEvent(attempted, new DisabledException("disabled")));

        verifyNoInteractions(loginAttemptService);
        verifyNoInteractions(gymMetrics);
    }
}
