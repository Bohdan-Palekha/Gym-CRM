package com.gymcrm.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

//@Async on TrainerWorkloadEventListener
//workload notifications run off the request thread and off hibernate's transaction-commit thread
//small bounded pool, not unbounded default SimpleAsyncTaskExecutor
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String WORKLOAD_NOTIFICATION_EXECUTOR = "workloadNotificationExecutor";

    @Bean(WORKLOAD_NOTIFICATION_EXECUTOR)
    public Executor workloadNotificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("workload-notify-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }
}
