package com.gymcrm.util.impl;

import com.gymcrm.util.UsernameGenerator;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class UsernameGeneratorImpl implements UsernameGenerator {

    @Override
    public String generate(String firstName, String lastName,
                           Collection<String> existingUsernames) {

        if (firstName == null || firstName.isBlank())
            throw new IllegalArgumentException("First name must not be null or blank");
        if (lastName == null || lastName.isBlank())
            throw new IllegalArgumentException("Last name must not be null or blank");

        String base = firstName.trim() + "." + lastName.trim();

        if (!existingUsernames.contains(base)) return base;

        int serial = 1;
        String candidate;
        do { candidate = base + serial++; }
        while (existingUsernames.contains(candidate));

        return candidate;
    }
}
