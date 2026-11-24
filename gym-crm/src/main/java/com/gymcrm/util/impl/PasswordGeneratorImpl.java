package com.gymcrm.util.impl;

import com.gymcrm.annotation.Sensitive;
import com.gymcrm.util.PasswordGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class PasswordGeneratorImpl implements PasswordGenerator {

    private static final String CHARACTER_POOL =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final int PASSWORD_LENGTH = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Sensitive
    public String generate() {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(CHARACTER_POOL.charAt(SECURE_RANDOM.nextInt(CHARACTER_POOL.length())));
        }
        return sb.toString();
    }
}
