package com.gymcrm.client.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AsyncConfig")
class AsyncConfigTest {

    private final AsyncConfig config = new AsyncConfig();

    @Test
    @DisplayName("workloadNotificationExecutor is a bounded pool, not the unbounded default")
    void executor_isBoundedThreadPool() {
        Executor executor = config.workloadNotificationExecutor();

        assertInstanceOf(ThreadPoolTaskExecutor.class, executor);
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executor;
        assertEquals(2, pool.getCorePoolSize());
        assertEquals(8, pool.getMaxPoolSize());
        assertEquals("workload-notify-", pool.getThreadNamePrefix());

        pool.shutdown();
    }

    @Test
    @DisplayName("the executor actually runs submitted work, on a differently-named thread")
    void executor_runsSubmittedWork() throws Exception {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) config.workloadNotificationExecutor();
        String callingThreadName = Thread.currentThread().getName();

        CompletableFuture<String> executedOnThread = new CompletableFuture<>();
        pool.execute(() -> executedOnThread.complete(Thread.currentThread().getName()));

        String threadName = executedOnThread.get(5, TimeUnit.SECONDS);
        assertNotEquals(callingThreadName, threadName);
        assertTrue(threadName.startsWith("workload-notify-"));

        pool.shutdown();
    }
}
