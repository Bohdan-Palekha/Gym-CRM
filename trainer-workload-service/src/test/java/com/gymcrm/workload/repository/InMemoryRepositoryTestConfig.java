package com.gymcrm.workload.repository;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

//swaps the real mongo repository for fake in-memory in full-context tests that useTrainerWorkloadServiceImpl but not MongoDB
//@Primary for preference against real @Repository scanned MongoTrainerWorkloadRepository
@TestConfiguration
public class InMemoryRepositoryTestConfig {

    @Bean
    @Primary
    public TrainerWorkloadRepository trainerWorkloadRepository() {
        return new InMemoryTrainerWorkloadRepositoryFake();
    }
}
