package com.gymcrm.security;

import com.gymcrm.dao.UserDao;
import com.gymcrm.metrics.GymMetrics;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureDisabledEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

//security tasks 6
//adds brute-force counter and the auth metrics into spring authentication eventing,
//used by auth manager around authenticate() call in AuthController.login
//AuthenticationFailureBadCredentialsEvent increments the counter
//login attempts while locked does not increment the counter
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationEventListener {

    private final LoginAttemptService loginAttemptService;
    private final GymMetrics gymMetrics;
    private final UserDao userDao;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        loginAttemptService.loginSucceeded(username);
        if (event.getAuthentication().getPrincipal() instanceof UserPrincipal principal) {
            recordSuccessMetric(principal.getUser());
        }
        log.info("Login succeeded: username={}", username);
    }

    @EventListener
    @Transactional(readOnly = true)
    public void onBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        loginAttemptService.loginFailed(username);
        userDao.findByUsername(username)
                .ifPresentOrElse(this::recordFailureMetric, gymMetrics::recordUnknownUserAuthFailure);
        log.warn("Login failed (bad credentials): username={}", username);
    }

    //logged for visibility only, does not call loginAttemptService.loginFailed,
    @EventListener
    public void onLocked(AuthenticationFailureLockedEvent event) {
        log.warn("Login rejected - account temporarily locked: username={}",
                event.getAuthentication().getName());
    }

    @EventListener
    public void onDisabled(AuthenticationFailureDisabledEvent event) {
        log.warn("Login rejected - account is deactivated: username={}",
                event.getAuthentication().getName());
    }

    private void recordSuccessMetric(User user) {
        if (user instanceof Trainee) {
            gymMetrics.recordTraineeAuthSuccess();
        } else if (user instanceof Trainer) {
            gymMetrics.recordTrainerAuthSuccess();
        }
    }

    private void recordFailureMetric(User user) {
        if (user instanceof Trainee) {
            gymMetrics.recordTraineeAuthFailure();
        } else if (user instanceof Trainer) {
            gymMetrics.recordTrainerAuthFailure();
        }
    }
}
