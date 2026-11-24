package com.gymcrm.service;

import com.gymcrm.annotation.Sensitive;
import com.gymcrm.dao.UserDao;
import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.metrics.GymMetrics;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//service for validation of credentials, password changes
//comparison is through hash verification
//authentication() goes trough spring security AuthenticationManager for brute-force lockout

@Slf4j
@Service
public class AuthenticationService {

    private UserDao userDao;
    private UsernameService usernameService;
    private GymMetrics gymMetrics;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public void setUserDao(UserDao d) {
        this.userDao = d;
    }

    @Autowired
    public void setUsernameService(UsernameService s) {
        this.usernameService = s;
    }

    @Autowired
    public void setGymMetrics(GymMetrics m) {
        this.gymMetrics = m;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Sensitive
    @Transactional(readOnly = true)
    public void authenticate(String username, String password) {
        checkCredentials(username, password);
        log.debug("Authenticated: username={}", username);
    }

    @Sensitive
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        usernameService.validateName(newPassword, "newPassword");
        User user = checkCredentials(username, oldPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        userDao.update(user);
        log.info("Password changed for username={}", username);
    }

    private User checkCredentials(String username, String password) {
        User user = userDao.findByUsername(username).orElseThrow(() -> {
            gymMetrics.recordUnknownUserAuthFailure();
            return new EntityNotFoundException("User not found: " + username);
        });
        if (!passwordEncoder.matches(password, user.getPassword())) {
            recordFailure(user);
            throw new SecurityException("Invalid password for username: " + username);
        }
        recordSuccess(user);
        return user;
    }

    private void recordSuccess(User user) {
        if (user instanceof Trainee) {
            gymMetrics.recordTraineeAuthSuccess();
        } else if (user instanceof Trainer) {
            gymMetrics.recordTrainerAuthSuccess();
        }
    }

    private void recordFailure(User user) {
        if (user instanceof Trainee) {
            gymMetrics.recordTraineeAuthFailure();
        } else if (user instanceof Trainer) {
            gymMetrics.recordTrainerAuthFailure();
        }
    }
}
