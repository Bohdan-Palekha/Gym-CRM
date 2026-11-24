package com.gymcrm.facade;

import com.gymcrm.model.Trainee;
import com.gymcrm.model.Trainer;
import com.gymcrm.model.Training;
import com.gymcrm.model.TrainingType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GymCrmFacade {

    //trainee orig methods

    Trainee createTrainee(String firstName, String lastName,
                          LocalDate dateOfBirth, String address);

    Trainee updateTrainee(Trainee trainee);

    void deleteTrainee(Long id);

    Optional<Trainee> selectTrainee(Long id);

    Optional<Trainee> selectTraineeByUsername(String username);

    List<Trainee> selectAllTrainees();

    //trainee task 3 7 11 13 14 17 18

    void authenticateTrainee(String username, String password);

    void changeTraineePassword(String username, String oldPassword, String newPassword);

    Trainee activateDeactivateTrainee(String username, boolean isActive);

    void deleteTraineeByUsername(String username);

    List<Training> getTraineeTrainings(String username, LocalDate from, LocalDate to,
                                       String trainerName,
                                       TrainingType.TrainingTypeName trainingType);

    List<Trainer> getNotAssignedActiveTrainers(String traineeUsername);

    Trainee updateTraineeTrainers(String traineeUsername, List<String> trainerUsernames);

    //trainer orig methods

    Trainer createTrainer(String firstName, String lastName,
                          TrainingType.TrainingTypeName specialization);

    Trainer updateTrainer(Trainer trainer);

    Optional<Trainer> selectTrainer(Long id);

    Optional<Trainer> selectTrainerByUsername(String username);

    List<Trainer> selectAllTrainers();

    //trainer task 4 8 12 15

    void authenticateTrainer(String username, String password);

    void changeTrainerPassword(String username, String oldPassword, String newPassword);

    Trainer activateDeactivateTrainer(String username, boolean isActive);

    List<Training> getTrainerTrainings(String username, LocalDate from, LocalDate to,
                                       String traineeName);

    //training orig methods

    Training createTraining(Long traineeId, Long trainerId,
                            String trainingName, TrainingType trainingType,
                            LocalDate trainingDate, int durationMinutes);

    Optional<Training> selectTraining(Long id);

    List<Training> selectAllTrainings();

    List<Training> selectTrainingsByTrainee(Long traineeId);

    List<Training> selectTrainingsByTrainer(Long trainerId);

    //training task

    Training addTraining(String traineeUsername, String trainerUsername,
                         String trainingName, LocalDate trainingDate,
                         TrainingType.TrainingTypeName trainingType, int durationMinutes);
}
