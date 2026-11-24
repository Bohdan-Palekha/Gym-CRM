package com.gymcrm.workload.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TrainerWorkloadDocument")
class TrainerWorkloadDocumentTest {

    @Nested
    @DisplayName("blank / isNew (Persistable contract)")
    class BlankAndIsNew {

        @Test
        @DisplayName("blank() has no identity, no history and version=null")
        void blank_hasNoHistory() {
            TrainerWorkloadDocument document = TrainerWorkloadDocument.blank("bob.trainer");

            assertEquals("bob.trainer", document.username());
            assertEquals("bob.trainer", document.getId());
            assertNull(document.firstName());
            assertNull(document.status());
            assertTrue(document.years().isEmpty());
            assertEquals(0, document.trainingSummaryDurationFor(2026, 8));
        }

        @Test
        @DisplayName("a document with version=null is new")
        void nullVersion_isNew() {
            assertTrue(TrainerWorkloadDocument.blank("bob.trainer").isNew());
        }

        @Test
        @DisplayName("a document with a non-null version is not new")
        void nonNullVersion_isNotNew() {
            TrainerWorkloadDocument persisted =
                    new TrainerWorkloadDocument("bob.trainer", "Bob", "Jones", true, java.util.List.of(), 0L);

            assertFalse(persisted.isNew());
        }
    }

    @Nested
    @DisplayName("withAdjustedDuration")
    class WithAdjustedDuration {

        @Test
        @DisplayName("accumulates within the same year/month")
        void accumulatesWithinSameMonth() {
            TrainerWorkloadDocument document = TrainerWorkloadDocument.blank("bob.trainer")
                    .withAdjustedDuration(2026, 8, 60)
                    .withAdjustedDuration(2026, 8, 30);

            assertEquals(90, document.trainingSummaryDurationFor(2026, 8));
        }

        @Test
        @DisplayName("different months/years are tracked independently")
        void keepsMonthsIndependent() {
            TrainerWorkloadDocument document = TrainerWorkloadDocument.blank("bob.trainer")
                    .withAdjustedDuration(2026, 8, 60)
                    .withAdjustedDuration(2026, 9, 45)
                    .withAdjustedDuration(2025, 8, 15);

            assertEquals(60, document.trainingSummaryDurationFor(2026, 8));
            assertEquals(45, document.trainingSummaryDurationFor(2026, 9));
            assertEquals(15, document.trainingSummaryDurationFor(2025, 8));
        }

        @Test
        @DisplayName("an unrecorded year/month returns 0, not an exception")
        void unrecordedMonth_returnsZero() {
            TrainerWorkloadDocument document = TrainerWorkloadDocument.blank("bob.trainer");
            assertEquals(0, document.trainingSummaryDurationFor(2026, 1));
        }

        @Test
        @DisplayName("a negative delta reduces the total")
        void negativeDelta_reduces() {
            TrainerWorkloadDocument document = TrainerWorkloadDocument.blank("bob.trainer")
                    .withAdjustedDuration(2026, 8, 90)
                    .withAdjustedDuration(2026, 8, -30);

            assertEquals(60, document.trainingSummaryDurationFor(2026, 8));
        }

        @Test
        @DisplayName("a negative delta never drops a month below zero")
        void negativeDelta_clampsAtZero() {
            TrainerWorkloadDocument document = TrainerWorkloadDocument.blank("bob.trainer")
                    .withAdjustedDuration(2026, 8, 20)
                    .withAdjustedDuration(2026, 8, -50);

            assertEquals(0, document.trainingSummaryDurationFor(2026, 8));
        }

        @Test
        @DisplayName("a negative delta for a month never seen before is clamped to zero, not negative")
        void negativeDelta_onUnseenMonth_clampsAtZero() {
            TrainerWorkloadDocument document = TrainerWorkloadDocument.blank("bob.trainer")
                    .withAdjustedDuration(2026, 8, -30);

            assertEquals(0, document.trainingSummaryDurationFor(2026, 8));
        }

        @Test
        @DisplayName("is immutable: the original instance is unaffected by a later adjustment")
        void doesNotMutateOriginal() {
            TrainerWorkloadDocument original = TrainerWorkloadDocument.blank("bob.trainer")
                    .withAdjustedDuration(2026, 8, 60);

            TrainerWorkloadDocument updated = original.withAdjustedDuration(2026, 8, 30);

            assertEquals(60, original.trainingSummaryDurationFor(2026, 8));
            assertEquals(90, updated.trainingSummaryDurationFor(2026, 8));
        }
    }

    @Nested
    @DisplayName("withIdentity")
    class WithIdentity {

        @Test
        @DisplayName("overwrites firstName/lastName/status, keeps username/years/version")
        void overwritesIdentityOnly() {
            TrainerWorkloadDocument document = TrainerWorkloadDocument.blank("bob.trainer")
                    .withAdjustedDuration(2026, 8, 60)
                    .withIdentity("Bob", "Jones", true);

            assertEquals("Bob", document.firstName());
            assertEquals("Jones", document.lastName());
            assertTrue(document.status());
            assertEquals("bob.trainer", document.username());
            assertEquals(60, document.trainingSummaryDurationFor(2026, 8));
        }

        @Test
        @DisplayName("identity is refreshed on every call, even to a falsy status")
        void refreshedOnEveryCall() {
            TrainerWorkloadDocument document = TrainerWorkloadDocument.blank("bob.trainer")
                    .withIdentity("Bob", "Jones", true)
                    .withIdentity("Robert", "Jones", false);

            assertEquals("Robert", document.firstName());
            assertFalse(document.status());
        }
    }

    @Nested
    @DisplayName("defensive construction")
    class DefensiveConstruction {

        @Test
        @DisplayName("a null years list is normalized to empty, not left null")
        void nullYears_normalizedToEmpty() {
            TrainerWorkloadDocument document =
                    new TrainerWorkloadDocument("bob.trainer", "Bob", "Jones", true, null, null);

            assertTrue(document.years().isEmpty());
        }

        @Test
        @DisplayName("a null months list on a YearEntry is normalized to empty, not left null")
        void nullMonths_normalizedToEmpty() {
            TrainerWorkloadDocument.YearEntry yearEntry = new TrainerWorkloadDocument.YearEntry(2026, null);

            assertTrue(yearEntry.months().isEmpty());
        }

        @Test
        @DisplayName("toString() stays short: it summarizes the history instead of dumping it")
        void toString_isConciseSummary() {
            TrainerWorkloadDocument document = TrainerWorkloadDocument.blank("bob.trainer")
                    .withAdjustedDuration(2026, 8, 60)
                    .withAdjustedDuration(2025, 1, 30);

            String text = document.toString();

            assertTrue(text.contains("bob.trainer"));
            assertTrue(text.contains("years=2"));
        }
    }
}
