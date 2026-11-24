package com.gymcrm.workload.repository;

import java.util.List;
import java.util.Optional;

public interface TrainerWorkloadRepository {

    Optional<TrainerWorkloadDocument> findByUsername(String username);

    TrainerWorkloadDocument save(TrainerWorkloadDocument document);

    List<TrainerWorkloadDocument> findByFirstNameAndLastName(String firstName, String lastName);
}
