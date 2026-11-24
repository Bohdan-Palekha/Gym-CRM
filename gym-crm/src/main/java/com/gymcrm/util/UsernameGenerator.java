package com.gymcrm.util;

import java.util.Collection;

public interface UsernameGenerator {
    String generate(String firstName, String lastName, Collection<String> existingUsernames);
}
