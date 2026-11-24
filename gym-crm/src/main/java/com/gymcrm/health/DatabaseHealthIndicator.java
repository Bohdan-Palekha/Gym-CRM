package com.gymcrm.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.sql.Statement;

//verifies that hibernate/jdbc is reachable
//spring boot actuator auto-configs a generic DataSource bases db indicator
//but GymApplication excludes jpa/datasource config, manages hibernate manually
//thus sessionfactory ,same as for DAO, is used, catching ORM/session layer issues
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {

    private final SessionFactory sessionFactory;

    @Override
    public Health health() {
        long start = System.currentTimeMillis();
        try (Session session = sessionFactory.openSession()) {
            session.doWork(connection -> {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SELECT 1");
                }
            });
            long responseTimeMs = System.currentTimeMillis() - start;
            log.debug("Database health check succeeded in {}ms", responseTimeMs);
            return Health.up()
                    .withDetail("responseTimeMs", responseTimeMs)
                    .build();
        } catch (Exception ex) {
            log.error("Database health check failed", ex);
            return Health.down(ex)
                    .withDetail("responseTimeMs", System.currentTimeMillis() - start)
                    .build();
        }
    }
}
