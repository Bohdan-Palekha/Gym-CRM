package com.gymcrm.workload.service;

import com.gymcrm.workload.dto.ActionType;
import com.gymcrm.workload.dto.MonthlyTrainingHoursResponse;
import com.gymcrm.workload.dto.TrainerWorkloadRequest;
import com.gymcrm.workload.dto.TrainerWorkloadResponse;
import com.gymcrm.workload.exception.TrainerWorkloadNotFoundException;
import com.gymcrm.workload.exception.TrainerWorkloadPersistenceException;
import com.gymcrm.workload.repository.TrainerWorkloadDocument;
import com.gymcrm.workload.repository.TrainerWorkloadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrainerWorkloadServiceImpl")
class TrainerWorkloadServiceImplTest {

    private static final int MAX_SAVE_ATTEMPTS = 5;

    @Mock
    private TrainerWorkloadRepository repository;

    private TrainerWorkloadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TrainerWorkloadServiceImpl(repository, MAX_SAVE_ATTEMPTS);
    }

    @Nested
    @DisplayName("applyWorkloadChange")
    class ApplyWorkloadChange {

        @Test
        @DisplayName("no existing document -> a new one is created with the event's data")
        void noExistingDocument_createsNewOne() {
            when(repository.findByUsername("bob.trainer")).thenReturn(Optional.empty());
            ArgumentCaptor<TrainerWorkloadDocument> captor = ArgumentCaptor.forClass(TrainerWorkloadDocument.class);
            when(repository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            service.applyWorkloadChange(new TrainerWorkloadRequest(
                    "bob.trainer", "Bob", "Jones", true,
                    LocalDate.of(2026, 8, 15), 60, ActionType.ADD));

            TrainerWorkloadDocument saved = captor.getValue();
            assertEquals("bob.trainer", saved.username());
            assertEquals("Bob", saved.firstName());
            assertEquals("Jones", saved.lastName());
            assertTrue(saved.status());
            assertEquals(60, saved.trainingSummaryDurationFor(2026, 8));
        }

        @Test
        @DisplayName("an existing document -> ADD increases the target month's total")
        void existingDocument_addIncreasesMinutes() {
            TrainerWorkloadDocument existing = TrainerWorkloadDocument.blank("bob.trainer")
                    .withIdentity("Bob", "Jones", true)
                    .withAdjustedDuration(2026, 8, 30);
            when(repository.findByUsername("bob.trainer")).thenReturn(Optional.of(existing));
            ArgumentCaptor<TrainerWorkloadDocument> captor = ArgumentCaptor.forClass(TrainerWorkloadDocument.class);
            when(repository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            service.applyWorkloadChange(new TrainerWorkloadRequest(
                    "bob.trainer", "Bob", "Jones", true,
                    LocalDate.of(2026, 8, 15), 60, ActionType.ADD));

            assertEquals(90, captor.getValue().trainingSummaryDurationFor(2026, 8));
        }

        @Test
        @DisplayName("DELETE decreases the target month's total")
        void delete_decreasesMinutes() {
            TrainerWorkloadDocument existing = TrainerWorkloadDocument.blank("bob.trainer")
                    .withIdentity("Bob", "Jones", true)
                    .withAdjustedDuration(2026, 8, 90);
            when(repository.findByUsername("bob.trainer")).thenReturn(Optional.of(existing));
            ArgumentCaptor<TrainerWorkloadDocument> captor = ArgumentCaptor.forClass(TrainerWorkloadDocument.class);
            when(repository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            service.applyWorkloadChange(new TrainerWorkloadRequest(
                    "bob.trainer", "Bob", "Jones", true,
                    LocalDate.of(2026, 8, 15), 30, ActionType.DELETE));

            assertEquals(60, captor.getValue().trainingSummaryDurationFor(2026, 8));
        }

        @Test
        @DisplayName("DELETE never drops a month below zero, even for a month never recorded before")
        void delete_neverGoesNegative() {
            when(repository.findByUsername("bob.trainer")).thenReturn(Optional.empty());
            ArgumentCaptor<TrainerWorkloadDocument> captor = ArgumentCaptor.forClass(TrainerWorkloadDocument.class);
            when(repository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            service.applyWorkloadChange(new TrainerWorkloadRequest(
                    "bob.trainer", "Bob", "Jones", true,
                    LocalDate.of(2026, 8, 15), 30, ActionType.DELETE));

            assertEquals(0, captor.getValue().trainingSummaryDurationFor(2026, 8));
        }

        @Test
        @DisplayName("the year/month bucket is derived from trainingDate")
        void usesTrainingDateForBucketing() {
            when(repository.findByUsername("bob.trainer")).thenReturn(Optional.empty());
            ArgumentCaptor<TrainerWorkloadDocument> captor = ArgumentCaptor.forClass(TrainerWorkloadDocument.class);
            when(repository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            service.applyWorkloadChange(new TrainerWorkloadRequest(
                    "bob.trainer", "Bob", "Jones", true,
                    LocalDate.of(2025, 12, 31), 45, ActionType.ADD));

            assertEquals(45, captor.getValue().trainingSummaryDurationFor(2025, 12));
            assertEquals(0, captor.getValue().trainingSummaryDurationFor(2026, 12));
        }

        @Test
        @DisplayName("identity is refreshed on every event, even DELETE")
        void identityRefreshedOnDelete() {
            TrainerWorkloadDocument existing = TrainerWorkloadDocument.blank("bob.trainer")
                    .withIdentity("Bob", "Jones", true)
                    .withAdjustedDuration(2026, 8, 60);
            when(repository.findByUsername("bob.trainer")).thenReturn(Optional.of(existing));
            ArgumentCaptor<TrainerWorkloadDocument> captor = ArgumentCaptor.forClass(TrainerWorkloadDocument.class);
            when(repository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            service.applyWorkloadChange(new TrainerWorkloadRequest(
                    "bob.trainer", "Bob", "Jones", false,
                    LocalDate.of(2026, 8, 15), 60, ActionType.DELETE));

            assertFalse(captor.getValue().status());
        }

        @Test
        @DisplayName("retries on an optimistic-lock conflict and succeeds once it sees the latest version")
        void optimisticLockConflict_retriesThenSucceeds() {
            TrainerWorkloadDocument existing = TrainerWorkloadDocument.blank("bob.trainer")
                    .withIdentity("Bob", "Jones", true);
            when(repository.findByUsername("bob.trainer")).thenReturn(Optional.of(existing));
            when(repository.save(any()))
                    .thenThrow(new OptimisticLockingFailureException("stale version"))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            assertDoesNotThrow(() -> service.applyWorkloadChange(new TrainerWorkloadRequest(
                    "bob.trainer", "Bob", "Jones", true,
                    LocalDate.of(2026, 8, 15), 60, ActionType.ADD)));

            verify(repository, times(2)).findByUsername("bob.trainer");
            verify(repository, times(2)).save(any());
        }

        @Test
        @DisplayName("gives up after repeated optimistic-lock conflicts rather than retrying forever")
        void optimisticLockConflict_exhaustsRetries() {
            TrainerWorkloadDocument existing = TrainerWorkloadDocument.blank("bob.trainer")
                    .withIdentity("Bob", "Jones", true);
            when(repository.findByUsername("bob.trainer")).thenReturn(Optional.of(existing));
            when(repository.save(any())).thenThrow(new OptimisticLockingFailureException("stale version"));

            TrainerWorkloadRequest request = new TrainerWorkloadRequest(
                    "bob.trainer", "Bob", "Jones", true,
                    LocalDate.of(2026, 8, 15), 60, ActionType.ADD);

            assertThrows(TrainerWorkloadPersistenceException.class,
                    () -> service.applyWorkloadChange(request));
            verify(repository, times(MAX_SAVE_ATTEMPTS)).save(any());
        }
    }

    @Nested
    @DisplayName("getMonthlySummary")
    class GetMonthlySummary {

        @Test
        @DisplayName("returns the recorded total for an existing trainer/month")
        void returnsRecordedTotal() {
            TrainerWorkloadDocument document = TrainerWorkloadDocument.blank("bob.trainer")
                    .withIdentity("Bob", "Jones", true)
                    .withAdjustedDuration(2026, 8, 90);
            when(repository.findByUsername("bob.trainer")).thenReturn(Optional.of(document));

            MonthlyTrainingHoursResponse result = service.getMonthlySummary("bob.trainer", 2026, 8);

            assertEquals("bob.trainer", result.trainerUsername());
            assertEquals("Bob", result.trainerFirstName());
            assertEquals("Jones", result.trainerLastName());
            assertTrue(result.trainerStatus());
            assertEquals(2026, result.year());
            assertEquals(8, result.month());
            assertEquals(90, result.trainingSummaryDuration());
        }

        @Test
        @DisplayName("a month with no recorded training returns 0, not a 404")
        void unrecordedMonth_returnsZero() {
            TrainerWorkloadDocument document = TrainerWorkloadDocument.blank("bob.trainer")
                    .withIdentity("Bob", "Jones", true)
                    .withAdjustedDuration(2026, 8, 90);
            when(repository.findByUsername("bob.trainer")).thenReturn(Optional.of(document));

            MonthlyTrainingHoursResponse result = service.getMonthlySummary("bob.trainer", 2026, 1);

            assertEquals(0, result.trainingSummaryDuration());
        }

        @Test
        @DisplayName("an unknown trainer raises TrainerWorkloadNotFoundException")
        void unknownTrainer_throws() {
            when(repository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThrows(TrainerWorkloadNotFoundException.class,
                    () -> service.getMonthlySummary("ghost", 2026, 8));
        }
    }

    @Nested
    @DisplayName("getFullSummary")
    class GetFullSummary {

        @Test
        @DisplayName("maps the document's full year/month history, newest year first, months ascending, "
                + "zero-duration months omitted")
        void mapsFullHistory() {
            TrainerWorkloadDocument document = TrainerWorkloadDocument.blank("bob.trainer")
                    .withIdentity("Bob", "Jones", true)
                    .withAdjustedDuration(2025, 12, 30)
                    .withAdjustedDuration(2026, 3, 20)
                    .withAdjustedDuration(2026, 1, 45)
                    .withAdjustedDuration(2026, 6, 10)
                    .withAdjustedDuration(2026, 6, -10); // back to zero -> should be omitted
            when(repository.findByUsername("bob.trainer")).thenReturn(Optional.of(document));

            TrainerWorkloadResponse result = service.getFullSummary("bob.trainer");

            assertEquals("bob.trainer", result.trainerUsername());
            assertEquals(2, result.years().size());
            assertEquals(2026, result.years().get(0).year());
            assertEquals(2, result.years().get(0).months().size());
            assertEquals(1, result.years().get(0).months().get(0).month());
            assertEquals(45, result.years().get(0).months().get(0).trainingSummaryDuration());
            assertEquals(3, result.years().get(0).months().get(1).month());
            assertEquals(2025, result.years().get(1).year());
            assertEquals(List.of(30), result.years().get(1).months().stream()
                    .map(month -> month.trainingSummaryDuration()).toList());
        }

        @Test
        @DisplayName("an unknown trainer raises TrainerWorkloadNotFoundException")
        void unknownTrainer_throws() {
            when(repository.findByUsername("ghost")).thenReturn(Optional.empty());
            assertThrows(TrainerWorkloadNotFoundException.class, () -> service.getFullSummary("ghost"));
        }
    }
}
