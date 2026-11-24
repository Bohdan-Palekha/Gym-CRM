package com.gymcrm.runner;

import com.gymcrm.exception.EntityNotFoundException;
import com.gymcrm.facade.GymCrmFacade;
import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.Training;
import com.gymcrm.model.TrainingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

//the original demo for Hybernate
//ran by mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataRunner implements CommandLineRunner {

    private final GymCrmFacade facade;

    @Override
    public void run(String... args) {

        log.info("Hibernate module demo");

        log.info("0. INITIAL STATE");
        log.info("Trainers={}, Trainees={}, Trainings={}",
                facade.selectAllTrainers().size(),
                facade.selectAllTrainees().size(),
                facade.selectAllTrainings().size());

        log.info("1. DATA POPULATION");

        log.info("Creating trainers");
        Trainer trYoga = facade.createTrainer("Alice", "Johnson", TrainingType.TrainingTypeName.YOGA);
        Trainer trFitness = facade.createTrainer("Bob", "Miller", TrainingType.TrainingTypeName.FITNESS);
        Trainer trZumba = facade.createTrainer("Carol", "Davis", TrainingType.TrainingTypeName.ZUMBA);
        Trainer trStretch = facade.createTrainer("David", "Wilson", TrainingType.TrainingTypeName.STRETCHING);
        Trainer trResist = facade.createTrainer("Eve", "Moore", TrainingType.TrainingTypeName.RESISTANCE);
        Trainer trYoga2 = facade.createTrainer("Alice", "Johnson", TrainingType.TrainingTypeName.YOGA);

        log.info("Trainer 1 : {} — {}", trYoga.getUsername(), trYoga.getSpecialization().getTrainingTypeName());
        log.info("Trainer 2 : {} — {}", trFitness.getUsername(), trFitness.getSpecialization().getTrainingTypeName());
        log.info("Trainer 3 : {} — {}", trZumba.getUsername(), trZumba.getSpecialization().getTrainingTypeName());
        log.info("Trainer 4 : {} — {}", trStretch.getUsername(), trStretch.getSpecialization().getTrainingTypeName());
        log.info("Trainer 5 : {} — {}", trResist.getUsername(), trResist.getSpecialization().getTrainingTypeName());
        log.info("Trainer 6 : {} — {} (collision → suffix added)",
                trYoga2.getUsername(), trYoga2.getSpecialization().getTrainingTypeName());

        log.info("Creating trainees");
        Trainee teeAlpha = facade.createTrainee("John", "Smith", LocalDate.of(1990, 4, 15), "12 Elm Street");
        Trainee teeBeta = facade.createTrainee("Maria", "Garcia", LocalDate.of(1995, 8, 22), "34 Oak Avenue");
        Trainee teeGamma = facade.createTrainee("Liam", "Brown", LocalDate.of(1988, 1, 30), "56 Pine Road");
        Trainee teeDelta = facade.createTrainee("Emma", "Taylor", LocalDate.of(2000, 11, 5), "78 Maple Lane");
        Trainee teeJohn2 = facade.createTrainee("John", "Smith", LocalDate.of(1993, 6, 10), "90 Birch Blvd");

        log.info("Trainee 1 : {} | DOB={} | addr={}", teeAlpha.getUsername(), teeAlpha.getDateOfBirth(), teeAlpha.getAddress());
        log.info("Trainee 2 : {} | DOB={}", teeBeta.getUsername(), teeBeta.getDateOfBirth());
        log.info("Trainee 3 : {} | DOB={}", teeGamma.getUsername(), teeGamma.getDateOfBirth());
        log.info("Trainee 4 : {} | DOB={}", teeDelta.getUsername(), teeDelta.getDateOfBirth());
        log.info("Trainee 5 : {} (collision → suffix added)", teeJohn2.getUsername());

        log.info("Assigning trainers (trainee_trainer join table)");
        facade.updateTraineeTrainers(teeAlpha.getUsername(), List.of(trYoga.getUsername(), trFitness.getUsername()));
        facade.updateTraineeTrainers(teeBeta.getUsername(), List.of(trZumba.getUsername(), trResist.getUsername()));
        facade.updateTraineeTrainers(teeGamma.getUsername(), List.of(trFitness.getUsername(), trStretch.getUsername(), trResist.getUsername()));
        facade.updateTraineeTrainers(teeDelta.getUsername(), List.of(trYoga.getUsername()));
        log.info("Trainer assignments committed for 4 trainees");

        log.info("Creating training sessions");
        facade.addTraining(teeAlpha.getUsername(), trYoga.getUsername(), "Morning Yoga Flow", LocalDate.of(2024, 1, 10), TrainingType.TrainingTypeName.YOGA, 60);
        facade.addTraining(teeAlpha.getUsername(), trFitness.getUsername(), "Core Strength Circuit", LocalDate.of(2024, 2, 5), TrainingType.TrainingTypeName.FITNESS, 45);
        facade.addTraining(teeBeta.getUsername(), trZumba.getUsername(), "Salsa Zumba", LocalDate.of(2024, 2, 14), TrainingType.TrainingTypeName.ZUMBA, 50);
        facade.addTraining(teeBeta.getUsername(), trResist.getUsername(), "Resistance Band Session", LocalDate.of(2024, 3, 1), TrainingType.TrainingTypeName.RESISTANCE, 40);
        facade.addTraining(teeGamma.getUsername(), trFitness.getUsername(), "HIIT Blast", LocalDate.of(2024, 3, 18), TrainingType.TrainingTypeName.FITNESS, 30);
        facade.addTraining(teeGamma.getUsername(), trStretch.getUsername(), "Deep Stretch", LocalDate.of(2024, 4, 2), TrainingType.TrainingTypeName.STRETCHING, 55);
        facade.addTraining(teeDelta.getUsername(), trYoga.getUsername(), "Sunset Yoga", LocalDate.of(2024, 4, 20), TrainingType.TrainingTypeName.YOGA, 75);
        facade.addTraining(teeAlpha.getUsername(), trYoga.getUsername(), "Advanced Yoga", LocalDate.of(2024, 5, 3), TrainingType.TrainingTypeName.YOGA, 90);
        Training s9 = facade.addTraining(teeJohn2.getUsername(), trFitness.getUsername(), "Intro Fitness", LocalDate.of(2024, 5, 15), TrainingType.TrainingTypeName.FITNESS, 30);
        log.info("Created 9 training sessions. s9='{}' owned by {} (cascade-delete demo)",
                s9.getTrainingName(), teeJohn2.getUsername());

        log.info("2. CRUD OPERATIONS");

        log.info("Update trainee address");
        teeAlpha.setAddress("999 Updated Boulevard");
        log.info("Updated {} → addr={}", facade.updateTrainee(teeAlpha).getUsername(), teeAlpha.getAddress());

        log.info("Update trainer first name — direct field access (no getUser())");
        trZumba.setFirstName("Caroline");
        log.info("Updated trainer → firstName={}", facade.updateTrainer(trZumba).getFirstName());

        log.info("Activate / De-activate");
        log.info("Deactivated {} → isActive={}", teeBeta.getUsername(),
                facade.activateDeactivateTrainee(teeBeta.getUsername(), false).isActive());
        log.info("Reactivated {} → isActive={}", teeBeta.getUsername(),
                facade.activateDeactivateTrainee(teeBeta.getUsername(), true).isActive());
        log.info("Deactivated trainer {} → isActive={}", trYoga2.getUsername(),
                facade.activateDeactivateTrainer(trYoga2.getUsername(), false).isActive());

        log.info("Password change");
        String originalPass = teeAlpha.getPassword();
        facade.changeTraineePassword(teeAlpha.getUsername(), originalPass, "NewSecurePass#99");
        teeAlpha = facade.selectTraineeByUsername(teeAlpha.getUsername()).orElseThrow();
        log.info("Password changed for trainee {}", teeAlpha.getUsername());

        log.info("Authentication — valid trainer credentials");
        facade.authenticateTrainer(trFitness.getUsername(), trFitness.getPassword());
        log.info("Trainer {} authenticated successfully", trFitness.getUsername());

        log.info("Authentication — invalid credentials (expected SecurityException)");
        try {
            facade.authenticateTrainee(teeAlpha.getUsername(), "wrongPassword");
        } catch (SecurityException e) {
            log.warn("Expected auth failure: {}", e.getMessage());
        }

        log.info("Select by username — present and absent");
        facade.selectTraineeByUsername(teeBeta.getUsername())
                .ifPresent(t -> log.info("Found {} id={}", t.getUsername(), t.getId()));
        log.info("Unknown lookup → {}",
                facade.selectTraineeByUsername("nobody.here").isEmpty() ? "empty (expected)" : "ERROR");

        log.info("Delete non-existent trainee (expected EntityNotFoundException)");
        try {
            facade.deleteTrainee(99999L);
        } catch (EntityNotFoundException e) {
            log.error("Expected: {}", e.getMessage());
        }

        log.info("3. CRITERIA-FILTERED QUERIES");

        List<Training> t14 = facade.getTraineeTrainings(teeAlpha.getUsername(),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31),
                "Alice", TrainingType.TrainingTypeName.YOGA);
        log.info("Task 14 — YOGA/Alice/2024 for '{}': {} session(s)", teeAlpha.getUsername(), t14.size());
        t14.forEach(t -> log.info("  → '{}' | {} | {} min", t.getTrainingName(), t.getTrainingDate(), t.getDurationMinutes()));

        List<Training> t15 = facade.getTrainerTrainings(trYoga.getUsername(),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), "John");
        log.info("Task 15 — trainer '{}' with trainee 'John': {} session(s)", trYoga.getUsername(), t15.size());

        List<Trainer> t17 = facade.getNotAssignedActiveTrainers(teeJohn2.getUsername());
        log.info("Task 17 — unassigned active trainers for '{}': {}", teeJohn2.getUsername(), t17.size());
        t17.forEach(t -> log.info("  → {} ({})", t.getUsername(), t.getSpecialization().getTrainingTypeName()));

        facade.updateTraineeTrainers(teeJohn2.getUsername(), List.of(trYoga.getUsername(), trStretch.getUsername()));
        log.info("Task 18 — updated trainer list for '{}'", teeJohn2.getUsername());

        log.info("4. CASCADE DELETE (Task 13)");

        int before = facade.selectAllTrainings().size();
        facade.deleteTraineeByUsername(teeJohn2.getUsername());
        int after = facade.selectAllTrainings().size();
        log.info("Cascade removed {} training(s) for deleted trainee", before - after);
        log.info("Lookup → {}", facade.selectTraineeByUsername(teeJohn2.getUsername()).isEmpty()
                ? "correctly absent" : "ERROR: still present!");

        log.info("FINAL STATE");
        log.info("Trainers={} | Trainees={} | Trainings={}",
                facade.selectAllTrainers().size(),
                facade.selectAllTrainees().size(),
                facade.selectAllTrainings().size());
        log.info("Gym CRM Hibernate module demo complete.");
    }
}
