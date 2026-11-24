package com.gymcrm.service;

import com.gymcrm.exception.InvalidStateException;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


@DisplayName("UserActivationRule")
class UserActivationRuleTest {

    @Test
    @DisplayName("flips an inactive user to active")
    void activatesInactiveUser() {
        Trainee trainee = newTrainee("alice.nguyen", false);

        Trainee result = UserActivationRule.apply(trainee, true);

        assertTrue(result.isActive());
        assertSame(trainee, result, "should mutate and return the same instance, not a copy");
    }

    @Test
    @DisplayName("flips an active user to inactive")
    void deactivatesActiveUser() {
        Trainee trainee = newTrainee("alice.nguyen", true);

        Trainee result = UserActivationRule.apply(trainee, false);

        assertFalse(result.isActive());
    }

    @Test
    @DisplayName("rejects activating a user who is already active (not idempotent)")
    void rejectsActivatingAlreadyActiveUser() {
        Trainee trainee = newTrainee("alice.nguyen", true);

        InvalidStateException ex = assertThrows(InvalidStateException.class,
                () -> UserActivationRule.apply(trainee, true));

        assertTrue(ex.getMessage().contains("alice.nguyen"));
        assertTrue(ex.getMessage().toLowerCase().contains("already active"));
        assertTrue(trainee.isActive(), "state must be untouched when the call is rejected");
    }

    @Test
    @DisplayName("rejects deactivating a user who is already inactive (not idempotent)")
    void rejectsDeactivatingAlreadyInactiveUser() {
        Trainee trainee = newTrainee("alice.nguyen", false);

        InvalidStateException ex = assertThrows(InvalidStateException.class,
                () -> UserActivationRule.apply(trainee, false));

        assertTrue(ex.getMessage().toLowerCase().contains("already inactive"));
        assertFalse(trainee.isActive(), "state must be untouched when the call is rejected");
    }

    @Test
    @DisplayName("applies identically to a Trainer as to a Trainee — the rule is generic over User (RESTtask criterion 8)")
    void appliesIdenticallyToTrainer() {
        Trainer trainer = new Trainer();
        trainer.setUsername("marcus.lee");
        trainer.setActive(false);

        Trainer result = UserActivationRule.apply(trainer, true);

        assertTrue(result.isActive());
        assertThrows(InvalidStateException.class, () -> UserActivationRule.apply(trainer, true));
    }

    private static Trainee newTrainee(String username, boolean active) {
        Trainee trainee = new Trainee();
        trainee.setUsername(username);
        trainee.setActive(active);
        return trainee;
    }
}
