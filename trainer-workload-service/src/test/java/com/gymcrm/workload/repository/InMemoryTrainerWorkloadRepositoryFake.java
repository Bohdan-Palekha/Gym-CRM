package com.gymcrm.workload.repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//stand-in for MongoTrainerWorkloadRepository, used by SecurityIntegrationTest, TrainerWorkloadMessagingIntegrationTest
//so they can use the TrainerWorkloadServiceImpl without MongoDB
//same search/update-by-username contract, but no lock conflicts
public class InMemoryTrainerWorkloadRepositoryFake implements TrainerWorkloadRepository {

    private final ConcurrentMap<String, TrainerWorkloadDocument> documentsByUsername = new ConcurrentHashMap<>();

    @Override
    public Optional<TrainerWorkloadDocument> findByUsername(String username) {
        return Optional.ofNullable(documentsByUsername.get(username));
    }

    @Override
    public TrainerWorkloadDocument save(TrainerWorkloadDocument document) {
        TrainerWorkloadDocument persisted =
                new TrainerWorkloadDocument(document.username(), document.firstName(), document.lastName(),
                        document.status(), document.years(), 0L);
        documentsByUsername.put(document.username(), persisted);
        return persisted;
    }

    @Override
    public List<TrainerWorkloadDocument> findByFirstNameAndLastName(String firstName, String lastName) {
        return documentsByUsername.values().stream()
                .filter(document -> document.firstName().equals(firstName) && document.lastName().equals(lastName))
                .toList();
    }
}
