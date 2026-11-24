package com.gymcrm.service;

import com.gymcrm.exception.InvalidStateException;
import com.gymcrm.model.User;

//applies state transition for users
//request with states that matches current are rejected
final class UserActivationRule {

    private UserActivationRule() {
    }

    static <T extends User> T apply(T user, boolean requestedActive) {
        if (user.isActive() == requestedActive) {
            String state = requestedActive ? "active" : "inactive";
            throw new InvalidStateException(
                    "User '%s' is already %s.".formatted(user.getUsername(), state));
        }
        user.setActive(requestedActive);
        return user;
    }
}
