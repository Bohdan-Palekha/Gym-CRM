package com.gymcrm.security;

import com.gymcrm.dao.UserDao;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    @Mock
    private UserDao userDao;
    @Mock
    private LoginAttemptService loginAttemptService;

    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new CustomUserDetailsService(userDao, loginAttemptService);
    }

    @Test
    @DisplayName("loads a UserPrincipal wrapping the trainee, unlocked when not brute-force blocked")
    void loadsTrainee_unlockedWhenNotBlocked() {
        Trainee trainee = new Trainee();
        trainee.setUsername("alice.nguyen");
        trainee.setPassword("$2a$hashed");
        trainee.setActive(true);
        when(userDao.findByUsername("alice.nguyen")).thenReturn(Optional.of(trainee));
        when(loginAttemptService.isBlocked("alice.nguyen")).thenReturn(false);

        UserDetails result = service.loadUserByUsername("alice.nguyen");

        assertInstanceOf(UserPrincipal.class, result);
        assertEquals("alice.nguyen", result.getUsername());
        assertEquals("$2a$hashed", result.getPassword());
        assertTrue(result.isAccountNonLocked());
        assertTrue(result.isEnabled());
        assertTrue(result.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TRAINEE")));
    }

    @Test
    @DisplayName("loads a UserPrincipal wrapping the trainer, with the trainer authority")
    void loadsTrainer_withTrainerAuthority() {
        Trainer trainer = new Trainer();
        trainer.setUsername("marcus.lee");
        trainer.setPassword("$2a$hashed");
        trainer.setActive(true);
        when(userDao.findByUsername("marcus.lee")).thenReturn(Optional.of(trainer));
        when(loginAttemptService.isBlocked("marcus.lee")).thenReturn(false);

        UserDetails result = service.loadUserByUsername("marcus.lee");

        assertTrue(result.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TRAINER")));
    }

    @Test
    @DisplayName("security task 6 — a brute-force-blocked username loads as locked")
    void bruteForceBlocked_loadsAsLocked() {
        Trainee trainee = new Trainee();
        trainee.setUsername("alice.nguyen");
        trainee.setPassword("$2a$hashed");
        trainee.setActive(true);
        when(userDao.findByUsername("alice.nguyen")).thenReturn(Optional.of(trainee));
        when(loginAttemptService.isBlocked("alice.nguyen")).thenReturn(true);

        UserDetails result = service.loadUserByUsername("alice.nguyen");

        assertFalse(result.isAccountNonLocked());
    }

    @Test
    @DisplayName("criterion 5.3 — a deactivated user loads as disabled")
    void deactivatedUser_loadsAsDisabled() {
        Trainee trainee = new Trainee();
        trainee.setUsername("alice.nguyen");
        trainee.setPassword("$2a$hashed");
        trainee.setActive(false);
        when(userDao.findByUsername("alice.nguyen")).thenReturn(Optional.of(trainee));
        when(loginAttemptService.isBlocked("alice.nguyen")).thenReturn(false);

        UserDetails result = service.loadUserByUsername("alice.nguyen");

        assertFalse(result.isEnabled());
    }

    @Test
    @DisplayName("unknown username throws UsernameNotFoundException")
    void unknownUsername_throws() {
        when(userDao.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("ghost"));
    }
}
