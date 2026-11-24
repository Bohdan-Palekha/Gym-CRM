package com.gymcrm.security;

import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserPrincipal")
class UserPrincipalTest {

    private Trainee trainee(boolean active) {
        Trainee trainee = new Trainee();
        trainee.setUsername("alice.nguyen");
        trainee.setPassword("$2a$hashed");
        trainee.setActive(active);
        return trainee;
    }

    @Test
    @DisplayName("delegates username/password to the wrapped user")
    void delegatesUsernameAndPassword() {
        UserPrincipal principal = new UserPrincipal(trainee(true), true);
        assertEquals("alice.nguyen", principal.getUsername());
        assertEquals("$2a$hashed", principal.getPassword());
    }

    @Test
    @DisplayName("exposes the wrapped domain user via getUser()")
    void exposesWrappedUser() {
        Trainee trainee = trainee(true);
        UserPrincipal principal = new UserPrincipal(trainee, true);
        assertSame(trainee, principal.getUser());
    }

    @Test
    @DisplayName("a trainee gets ROLE_TRAINEE")
    void trainee_getsTraineeRole() {
        UserPrincipal principal = new UserPrincipal(trainee(true), true);
        assertTrue(principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TRAINEE")));
        assertEquals(1, principal.getAuthorities().size());
    }

    @Test
    @DisplayName("a trainer gets ROLE_TRAINER")
    void trainer_getsTrainerRole() {
        Trainer trainer = new Trainer();
        trainer.setUsername("marcus.lee");
        trainer.setPassword("$2a$hashed");
        trainer.setActive(true);

        UserPrincipal principal = new UserPrincipal(trainer, true);
        assertTrue(principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TRAINER")));
    }

    @Test
    @DisplayName("criterion 5.3 — isEnabled() reflects the active flag, both ways")
    void isEnabled_reflectsActiveFlag() {
        assertTrue(new UserPrincipal(trainee(true), true).isEnabled());
        assertFalse(new UserPrincipal(trainee(false), true).isEnabled());
    }

    @Test
    @DisplayName("security task 6 — isAccountNonLocked() reflects the value given at construction, both ways")
    void isAccountNonLocked_reflectsConstructorValue() {
        assertTrue(new UserPrincipal(trainee(true), true).isAccountNonLocked());
        assertFalse(new UserPrincipal(trainee(true), false).isAccountNonLocked());
    }

    @Test
    @DisplayName("account is never expired and credentials are never expired")
    void neverExpires() {
        UserPrincipal principal = new UserPrincipal(trainee(true), true);
        assertTrue(principal.isAccountNonExpired());
        assertTrue(principal.isCredentialsNonExpired());
    }
}
