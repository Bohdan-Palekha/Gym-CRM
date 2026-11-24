package com.gymcrm.workload.service;

import com.gymcrm.workload.dto.MonthSummary;
import com.gymcrm.workload.dto.MonthlyTrainingHoursResponse;
import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import com.gymcrm.workload.dto.TrainerWorkloadResponse;
import com.gymcrm.workload.dto.YearSummary;
import com.gymcrm.workload.exception.TrainerWorkloadNotFoundException;
import com.gymcrm.workload.exception.TrainerWorkloadPersistenceException;
import com.gymcrm.workload.repository.TrainerWorkloadDocument;
import com.gymcrm.workload.repository.TrainerWorkloadRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class TrainerWorkloadServiceImpl implements TrainerWorkloadService {

    private final TrainerWorkloadRepository repository;
    private final int maxSaveAttempts;

    public TrainerWorkloadServiceImpl(
            TrainerWorkloadRepository repository,
            @Value("${trainer-workload.mongo.max-save-attempts:5}") int maxSaveAttempts) {
        this.repository = repository;
        this.maxSaveAttempts = maxSaveAttempts;
    }

    @Override
    public void applyWorkloadChange(TrainerWorkloadRequest request) {
        String username = request.trainerUsername();
        int year = request.trainingDate().getYear();
        int month = request.trainingDate().getMonthValue();

        //no default, a new ActionType will need a method update
        int deltaMinutes = switch (request.actionType()) {
            case ADD -> request.trainingDuration();
            case DELETE -> -request.trainingDuration();
        };

        //read-modify-write loop, retried on optimistic-lock conflicts
        //JMS listener can run trainer-workload.updates consumers concurrently
        //two events for the same trainer/month can race each other
        //@Version on the document makes a stale write fail fast instead
        //no overwrite of concurrent update, read the latest copy and apply the delta
        for (int attempt = 1; attempt <= maxSaveAttempts; attempt++) {
            Optional<TrainerWorkloadDocument> existing = repository.findByUsername(username);
            boolean isNewTrainerRecord = existing.isEmpty();
            TrainerWorkloadDocument current = existing.orElseGet(() -> TrainerWorkloadDocument.blank(username));

            TrainerWorkloadDocument updated = current
                    .withIdentity(request.trainerFirstName(), request.trainerLastName(), request.isActive())
                    .withAdjustedDuration(year, month, deltaMinutes);

            try {
                repository.save(updated);
                log.info("Applied {} for trainer={} year={} month={} deltaMinutes={} newRecord={}",
                        request.actionType(), username, year, month, deltaMinutes, isNewTrainerRecord);
                return;
            } catch (OptimisticLockingFailureException conflict) {
                log.debug("Optimistic lock conflict saving workload for trainer={} (attempt {}/{}), retrying",
                        username, attempt, maxSaveAttempts);
            }
        }
        throw new TrainerWorkloadPersistenceException(
                "Could not persist workload change for trainer=" + username
                        + " after " + maxSaveAttempts + " attempts (concurrent updates kept conflicting)");
    }

    @Override
    public MonthlyTrainingHoursResponse getMonthlySummary(String username, int year, int month) {
        TrainerWorkloadDocument document = findExisting(username);
        return new MonthlyTrainingHoursResponse(
                document.username(),
                document.firstName(),
                document.lastName(),
                Boolean.TRUE.equals(document.status()),
                year,
                month,
                document.trainingSummaryDurationFor(year, month));
    }

    @Override
    public TrainerWorkloadResponse getFullSummary(String username) {
        TrainerWorkloadDocument document = findExisting(username);
        List<YearSummary> years = document.years().stream()
                .sorted(Comparator.comparingInt(TrainerWorkloadDocument.YearEntry::year).reversed())
                .map(yearEntry -> new YearSummary(yearEntry.year(), toMonthSummaries(yearEntry.months())))
                .toList();

        return new TrainerWorkloadResponse(
                document.username(),
                document.firstName(),
                document.lastName(),
                Boolean.TRUE.equals(document.status()),
                years);
    }

    private TrainerWorkloadDocument findExisting(String username) {
        return repository.findByUsername(username).orElseThrow(() ->
                new TrainerWorkloadNotFoundException("No workload recorded for trainer: " + username));
    }

    private List<MonthSummary> toMonthSummaries(List<TrainerWorkloadDocument.MonthEntry> months) {
        return months.stream()
                .filter(monthEntry -> monthEntry.trainingSummaryDuration() > 0)
                .sorted(Comparator.comparingInt(TrainerWorkloadDocument.MonthEntry::month))
                .map(monthEntry -> new MonthSummary(monthEntry.month(), monthEntry.trainingSummaryDuration()))
                .toList();
    }
}
