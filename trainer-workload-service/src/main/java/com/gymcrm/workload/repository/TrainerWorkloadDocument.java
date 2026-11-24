package com.gymcrm.workload.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

//mongo schema for a trainer training summary
//username is natural key, gym-crm identifies a trainer by it, thus it is used as _id
//immutable, every change through method returns a new snapshot,
//the caller's service layer decides if/when to persist
@Document(collection = "trainer_workloads")
@CompoundIndex(name = "firstName_lastName_idx", def = "{'firstName': 1, 'lastName': 1}")
public record TrainerWorkloadDocument(

        @Id
        @NotBlank(message = "username must not be blank")
        String username,

        @NotBlank(message = "firstName must not be blank")
        String firstName,

        @NotBlank(message = "lastName must not be blank")
        String lastName,

        @NotNull(message = "status must not be null")
        Boolean status,

        @NotNull(message = "years must not be null")
        @Valid
        List<YearEntry> years,

        //assigned non-null id + @Version
        //this class spring data how to detect new, not relying on id null
        //which would be false and would make first save for a trainer look like an update
        @Version
        Long version
) implements Persistable<String> {

    public TrainerWorkloadDocument {
        years = (years == null) ? List.of() : List.copyOf(years);
    }

    public static TrainerWorkloadDocument blank(String username) {
        return new TrainerWorkloadDocument(username, null, null, null, List.of(), null);
    }

    @Override
    public String getId() {
        return username;
    }

    @Override
    public boolean isNew() {
        return version == null;
    }

    //refreshes the mutable identity fields
    //keeps username/years/version
    public TrainerWorkloadDocument withIdentity(String newFirstName, String newLastName, boolean newStatus) {
        return new TrainerWorkloadDocument(username, newFirstName, newLastName, newStatus, years, version);
    }

    //locate (or create) the year -> month bucket and add deltaMinutes to it
    //clamped at zero, never negative
    public TrainerWorkloadDocument withAdjustedDuration(int year, int month, int deltaMinutes) {
        List<YearEntry> updatedYears = new ArrayList<>(years);
        int yearIndex = indexWhere(updatedYears, entry -> entry.year() == year);

        if (yearIndex < 0) {
            updatedYears.add(new YearEntry(year, List.of(new MonthEntry(month, clampAtZero(deltaMinutes)))));
            return new TrainerWorkloadDocument(username, firstName, lastName, status, updatedYears, version);
        }

        YearEntry targetYear = updatedYears.get(yearIndex);
        List<MonthEntry> updatedMonths = new ArrayList<>(targetYear.months());
        int monthIndex = indexWhere(updatedMonths, entry -> entry.month() == month);

        if (monthIndex < 0) {
            updatedMonths.add(new MonthEntry(month, clampAtZero(deltaMinutes)));
        } else {
            int newDuration = clampAtZero(updatedMonths.get(monthIndex).trainingSummaryDuration() + deltaMinutes);
            updatedMonths.set(monthIndex, new MonthEntry(month, newDuration));
        }
        updatedYears.set(yearIndex, new YearEntry(year, updatedMonths));
        return new TrainerWorkloadDocument(username, firstName, lastName, status, updatedYears, version);
    }

    //used by the single month query, unlike full history view, never omits a zero
    public int trainingSummaryDurationFor(int year, int month) {
        return years.stream()
                .filter(entry -> entry.year() == year)
                .findFirst()
                .flatMap(entry -> entry.months().stream()
                        .filter(month1 -> month1.month() == month)
                        .findFirst())
                .map(MonthEntry::trainingSummaryDuration)
                .orElse(0);
    }

    private static int clampAtZero(int value) {
        return Math.max(0, value);
    }

    private static <T> int indexWhere(List<T> list, Predicate<T> predicate) {
        for (int i = 0; i < list.size(); i++) {
            if (predicate.test(list.get(i))) {
                return i;
            }
        }
        return -1;
    }

    //aop logging aspect logs this record's toString() on repository call
    //default record toString would dump the entire year/month history
    @Override
    public String toString() {
        return "TrainerWorkloadDocument[username=%s, years=%d, version=%s]"
                .formatted(username, years.size(), version);
    }

    public record YearEntry(

            @Min(value = 2000, message = "year must be 2000 or later")
            @Max(value = 2100, message = "year must be 2100 or earlier")
            int year,

            @NotNull(message = "months must not be null")
            @Valid
            List<MonthEntry> months
    ) {
        public YearEntry {
            months = (months == null) ? List.of() : List.copyOf(months);
        }
    }

    public record MonthEntry(

            @Min(value = 1, message = "month must be between 1 and 12")
            @Max(value = 12, message = "month must be between 1 and 12")
            int month,

            @PositiveOrZero(message = "trainingSummaryDuration must not be negative")
            int trainingSummaryDuration
    ) {
    }
}
