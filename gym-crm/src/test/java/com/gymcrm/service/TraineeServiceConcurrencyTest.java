package com.gymcrm.service;

import com.gymcrm.TestHibernateConfig;
import com.gymcrm.facade.GymCrmFacade;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.TrainingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestHibernateConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Username reservation — concurrency integration tests")
class TraineeServiceConcurrencyTest {

    @Autowired
    private GymCrmFacade facade;

    @Test
    @DisplayName("20 concurrent createTrainee calls produce 20 unique usernames")
    void concurrentCreateTrainee_uniqueUsernames() throws InterruptedException {
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<String> usernames = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    Trainee t = facade.createTrainee("John", "Smith", null, null);
                    usernames.add(t.getUsername());
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(0, errors.size(), "Errors: " + errors);
        assertEquals(threads, new HashSet<>(usernames).size(),
                "Duplicates found: " + findDuplicates(usernames));
    }

    @Test
    @DisplayName("Mixed Trainer+Trainee concurrent creation produces unique cross-entity usernames")
    void concurrentMixedCreate_uniqueAcrossEntities() throws InterruptedException {
        int each = 10;
        int total = each * 2;
        ExecutorService pool = Executors.newFixedThreadPool(total);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(total);
        List<String> names = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < each; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    Trainer t = facade.createTrainer("Alex", "Jones",
                            TrainingType.TrainingTypeName.YOGA);
                    names.add(t.getUsername());
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    done.countDown();
                }
            });
            pool.submit(() -> {
                try {
                    start.await();
                    Trainee t = facade.createTrainee("Alex", "Jones", null, null);
                    names.add(t.getUsername());
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(0, errors.size(), "Errors: " + errors);
        assertEquals(total, new HashSet<>(names).size(),
                "Cross-entity duplicates: " + findDuplicates(names));
    }

    private List<String> findDuplicates(List<String> list) {
        Set<String> seen = new HashSet<>();
        Set<String> dups = new HashSet<>();
        list.forEach(s -> {
            if (!seen.add(s)) dups.add(s);
        });
        return new ArrayList<>(dups);
    }
}
