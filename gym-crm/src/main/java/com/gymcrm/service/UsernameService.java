package com.gymcrm.service;

import com.gymcrm.util.UsernameGenerator;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//in-memory ConcurentHashMap is used for fast collision detection. On startup it is populated by users table
@Slf4j
@Service
public class UsernameService {

    private final Set<String> usernameRegistry = ConcurrentHashMap.newKeySet();

    private UsernameGenerator usernameGenerator;
    private SessionFactory sessionFactory;   // may be null in Mockito unit tests

    @Autowired
    public void setUsernameGenerator(UsernameGenerator g) {
        this.usernameGenerator = g;
    }

    @Autowired
    public void setSessionFactory(SessionFactory sf) {
        this.sessionFactory = sf;
    }

    //Raw Hibernate session is used PostConstruct fires before the transaction proxy is active
    @PostConstruct
    public void seedRegistryFromDb() {
        if (sessionFactory == null) return; // guard for Mockito tests
        try (Session session = sessionFactory.openSession()) {
            List<String> existing = session
                    .createQuery("SELECT u.username FROM User u", String.class)
                    .list();
            usernameRegistry.addAll(existing);
            log.info("Username registry seeded with {} existing username(s)", existing.size());
        } catch (Exception e) {
            log.warn("Could not seed username registry from DB (first run?): {}", e.getMessage());
        }
    }

    public void validateName(String value, String fieldName) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    //generate and reserve
    public String reserveUsername(String firstName, String lastName) {
        String candidate = usernameGenerator.generate(firstName, lastName, usernameRegistry);
        while (!usernameRegistry.add(candidate)) {
            candidate = usernameGenerator.generate(firstName, lastName, usernameRegistry);
        }
        return candidate;
    }
}
